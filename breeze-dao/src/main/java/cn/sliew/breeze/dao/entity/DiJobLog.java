package cn.sliew.breeze.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * <p>
 * 数据集成-作业运行日志
 * </p>
 *
 * @author liyu
 * @since 2022-05-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("di_job_log")
@ApiModel(value="DiJobLog对象", description="数据集成-作业运行日志")
public class DiJobLog extends BaseDO {

    private static final long serialVersionUID = -8679804488509252540L;

    @ApiModelProperty(value = "项目id")
    private Long projectId;

    @ApiModelProperty(value = "作业id")
    private Long jobId;

    @ApiModelProperty(value = "作业编码")
    private String jobCode;

    @ApiModelProperty(value = "执行集群id")
    private Long clusterId;

    @ApiModelProperty(value = "作业实例id")
    private String jobInstanceId;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "消耗时长-秒")
    private Long duration;

    @ApiModelProperty(value = "任务结果")
    private String jobInstanceState;


}