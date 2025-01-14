import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {PageResponse, ResponseBody} from '../../data/app.data';
import {
  FlinkDeployConfig,
  FlinkDeployConfigUploadParam,
  FlinkRelease,
  FlinkReleaseUploadParam
} from '../../data/flink.data';
import {DiProject, DiResourceFile} from "../../data/datadev.data";

@Injectable({
  providedIn: 'root',
})
export class DeployConfigService {
  private url = 'api/flink/deploy-config';

  constructor(private http: HttpClient) {
  }

  list(queryParam): Observable<PageResponse<FlinkDeployConfig>> {
    const params: HttpParams = new HttpParams({fromObject: queryParam});
    return this.http.get<PageResponse<FlinkDeployConfig>>(`${this.url}`, {params});
  }

  add(row: FlinkDeployConfig): Observable<ResponseBody<any>> {
    console.log(row)
    return this.http.put<ResponseBody<any>>(this.url, row);
  }

  update(row: FlinkDeployConfig): Observable<ResponseBody<any>> {
    return this.http.post<ResponseBody<any>>(this.url, row);
  }

  deleteBatch(rows: FlinkDeployConfig[]): Observable<ResponseBody<any>> {
    let params = rows.map((row) => row.id);
    return this.http.delete<ResponseBody<any>>(`${this.url}/batch`, {body: params});
  }

  upload(uploadParam: FlinkDeployConfigUploadParam): Observable<ResponseBody<any>> {
    const params: FormData = new FormData();
    params.append("configType", uploadParam.configType)
    params.append("name", uploadParam.name)
    uploadParam.files.forEach(function (file) {
      params.append("files", file)
    })
    params.append("remark", uploadParam.remark)
    return this.http.post<ResponseBody<any>>(`${this.url}/upload`, params);
  }


}
