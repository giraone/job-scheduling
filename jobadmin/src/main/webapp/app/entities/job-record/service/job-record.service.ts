import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IJobRecord, getJobRecordIdentifier } from '../job-record.model';

export type EntityResponseType = HttpResponse<IJobRecord>;
export type EntityArrayResponseType = HttpResponse<IJobRecord[]>;

@Injectable({ providedIn: 'root' })
export class JobRecordService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/job-records');

  constructor(protected http: HttpClient, protected applicationConfigService: ApplicationConfigService) {}

  create(jobRecord: IJobRecord): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(jobRecord);
    return this.http
      .post<IJobRecord>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  update(jobRecord: IJobRecord): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(jobRecord);
    return this.http
      .put<IJobRecord>(`${this.resourceUrl}/${getJobRecordIdentifier(jobRecord) as number}`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  partialUpdate(jobRecord: IJobRecord): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(jobRecord);
    return this.http
      .patch<IJobRecord>(`${this.resourceUrl}/${getJobRecordIdentifier(jobRecord) as number}`, copy, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<IJobRecord>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<IJobRecord[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  deleteAll(): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}-delete-all`, { observe: 'response' });
  }

  addJobRecordToCollectionIfMissing(
    jobRecordCollection: IJobRecord[],
    ...jobRecordsToCheck: (IJobRecord | null | undefined)[]
  ): IJobRecord[] {
    const jobRecords: IJobRecord[] = jobRecordsToCheck.filter(isPresent);
    if (jobRecords.length > 0) {
      const jobRecordCollectionIdentifiers = jobRecordCollection.map(jobRecordItem => getJobRecordIdentifier(jobRecordItem)!);
      const jobRecordsToAdd = jobRecords.filter(jobRecordItem => {
        const jobRecordIdentifier = getJobRecordIdentifier(jobRecordItem);
        if (jobRecordIdentifier == null || jobRecordCollectionIdentifiers.includes(jobRecordIdentifier)) {
          return false;
        }
        jobRecordCollectionIdentifiers.push(jobRecordIdentifier);
        return true;
      });
      return [...jobRecordsToAdd, ...jobRecordCollection];
    }
    return jobRecordCollection;
  }

  protected convertDateFromClient(jobRecord: IJobRecord): IJobRecord {
    return Object.assign({}, jobRecord, {
      jobAcceptedTimestamp: jobRecord.jobAcceptedTimestamp?.isValid() ? jobRecord.jobAcceptedTimestamp.toJSON() : undefined,
      lastEventTimestamp: jobRecord.lastEventTimestamp?.isValid() ? jobRecord.lastEventTimestamp.toJSON() : undefined,
      lastRecordUpdateTimestamp: jobRecord.lastRecordUpdateTimestamp?.isValid() ? jobRecord.lastRecordUpdateTimestamp.toJSON() : undefined,
    });
  }

  protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
    if (res.body) {
      res.body.jobAcceptedTimestamp = res.body.jobAcceptedTimestamp ? dayjs(res.body.jobAcceptedTimestamp) : undefined;
      res.body.lastEventTimestamp = res.body.lastEventTimestamp ? dayjs(res.body.lastEventTimestamp) : undefined;
      res.body.lastRecordUpdateTimestamp = res.body.lastRecordUpdateTimestamp ? dayjs(res.body.lastRecordUpdateTimestamp) : undefined;
    }
    return res;
  }

  protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
    if (res.body) {
      res.body.forEach((jobRecord: IJobRecord) => {
        jobRecord.jobAcceptedTimestamp = jobRecord.jobAcceptedTimestamp ? dayjs(jobRecord.jobAcceptedTimestamp) : undefined;
        jobRecord.lastEventTimestamp = jobRecord.lastEventTimestamp ? dayjs(jobRecord.lastEventTimestamp) : undefined;
        jobRecord.lastRecordUpdateTimestamp = jobRecord.lastRecordUpdateTimestamp ? dayjs(jobRecord.lastRecordUpdateTimestamp) : undefined;
      });
    }
    return res;
  }
}
