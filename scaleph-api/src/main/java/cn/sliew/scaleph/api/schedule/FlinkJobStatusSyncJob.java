package cn.sliew.scaleph.api.schedule;

import cn.hutool.core.util.StrUtil;
import cn.sliew.scaleph.common.constant.Constants;
import cn.sliew.scaleph.common.constant.DictConstants;
import cn.sliew.scaleph.common.enums.JobRuntimeStateEnum;
import cn.sliew.scaleph.service.di.DiClusterConfigService;
import cn.sliew.scaleph.service.di.DiJobLogService;
import cn.sliew.scaleph.service.di.DiJobService;
import cn.sliew.scaleph.service.dto.admin.ScheduleLogDTO;
import cn.sliew.scaleph.service.dto.di.DiClusterConfigDTO;
import cn.sliew.scaleph.service.dto.di.DiJobDTO;
import cn.sliew.scaleph.service.dto.di.DiJobLogDTO;
import cn.sliew.scaleph.service.vo.DictVO;
import cn.sliew.flinkful.rest.base.JobClient;
import cn.sliew.flinkful.rest.base.RestClient;
import cn.sliew.flinkful.rest.client.FlinkRestClient;
import org.apache.flink.api.common.JobStatus;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.rest.messages.job.JobDetailsInfo;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class FlinkJobStatusSyncJob extends QuartzJobBean {

    @Autowired
    private DiJobService diJobService;
    @Autowired
    private DiJobLogService diJobLogService;
    @Autowired
    private DiClusterConfigService diClusterConfigService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        ScheduleLogDTO logDTO = (ScheduleLogDTO) dataMap.get(Constants.JOB_LOG_KEY);
        logDTO.appendLog(StrUtil.format("start synchronization task status..."));
        List<DiJobLogDTO> list = this.diJobLogService.listRunningJobInstance(null);
        logDTO.appendLog(StrUtil.format("There are {} jobs is running", list.size()));
        Configuration configuration = GlobalConfiguration.loadConfiguration();
        for (DiJobLogDTO jobLog : list) {
            DiClusterConfigDTO clusterInfo = diClusterConfigService.selectOne(jobLog.getClusterId());
            String host = clusterInfo.getConfig().get(JobManagerOptions.ADDRESS.key());
            int restPort = Integer.parseInt(clusterInfo.getConfig().get(RestOptions.PORT.key()));
            RestClient client = new FlinkRestClient(host, restPort, configuration);
            JobClient jobClient = client.job();
            logDTO.appendLog(StrUtil.format("start synchronizing the job status,jobId = {},clusterHost = {},clusterPort = {}", jobLog.getJobInstanceId(), host, restPort));
            try {
                CompletableFuture<JobDetailsInfo> jobDetailsInfoFuture = jobClient.jobDetail(jobLog.getJobInstanceId());
                JobDetailsInfo jobDetailsInfo = jobDetailsInfoFuture.join();
                if (jobDetailsInfo != null) {
                    jobLog.setStartTime(new Date(jobDetailsInfo.getStartTime()));
                    if (jobDetailsInfo.getEndTime() > jobDetailsInfo.getStartTime()) {
                        jobLog.setEndTime(new Date(jobDetailsInfo.getEndTime()));
                    }
                    jobLog.setDuration(jobDetailsInfo.getDuration());
                    JobStatus jobStatus = jobDetailsInfo.getJobStatus();
                    jobLog.setJobInstanceState(DictVO.toVO(DictConstants.JOB_INSTANCE_STATE, jobStatus.toString()));
                    if (jobStatus.isGloballyTerminalState()) {
                        DiJobDTO diJobDTO = new DiJobDTO();
                        diJobDTO.setId(jobLog.getJobId());
                        diJobDTO.setRuntimeState(DictVO.toVO(DictConstants.RUNTIME_STATE, JobRuntimeStateEnum.STOP.getValue()));
                        this.diJobService.update(diJobDTO);
                    }
                    this.diJobLogService.update(jobLog);
                    logDTO.appendLog(StrUtil.format("success synchronizing job status of job {}", jobLog.getJobInstanceId()));
                } else {
                    logDTO.appendLog(StrUtil.format("job status of job {} is null", jobLog.getJobInstanceId()));
                }
            } catch (IOException e) {
                throw new JobExecutionException(StrUtil.format("exception to get jobDetailsInfo with JobId:{}", jobLog.getJobInstanceId()));
            }
        }
        logDTO.appendLog(StrUtil.format("finish synchronization task status..."));
    }
}