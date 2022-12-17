import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import dayjs from 'dayjs/esm';

import { isPresent } from 'app/core/util/operators';
import { ApplicationConfigService } from 'app/core/config/application-config.service';
import { createRequestOption } from 'app/core/request/request-util';
import { IJobRecord, NewJobRecord } from '../job-record.model';

export type PartialUpdateJobRecord = Partial<IJobRecord> & Pick<IJobRecord, 'id'>;

type RestOf<T extends IJobRecord | NewJobRecord> = Omit<T, 'jobAcceptedTimestamp' | 'lastEventTimestamp' | 'lastRecordUpdateTimestamp'> & {
  jobAcceptedTimestamp?: string | null;
  lastEventTimestamp?: string | null;
  lastRecordUpdateTimestamp?: string | null;
};

export type RestJobRecord = RestOf<IJobRecord>;

export type NewRestJobRecord = RestOf<NewJobRecord>;

export type PartialUpdateRestJobRecord = RestOf<PartialUpdateJobRecord>;

export type EntityResponseType = HttpResponse<IJobRecord>;
export type EntityArrayResponseType = HttpResponse<IJobRecord[]>;

@Injectable({ providedIn: 'root' })
export class JobRecordService {
  protected resourceUrl = this.applicationConfigService.getEndpointFor('api/job-records');

  constructor(protected http: HttpClient, protected applicationConfigService: ApplicationConfigService) {}

  create(jobRecord: NewJobRecord): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(jobRecord);
    return this.http
      .post<RestJobRecord>(this.resourceUrl, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  update(jobRecord: IJobRecord): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(jobRecord);
    return this.http
      .put<RestJobRecord>(`${this.resourceUrl}/${this.getJobRecordIdentifier(jobRecord)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  partialUpdate(jobRecord: PartialUpdateJobRecord): Observable<EntityResponseType> {
    const copy = this.convertDateFromClient(jobRecord);
    return this.http
      .patch<RestJobRecord>(`${this.resourceUrl}/${this.getJobRecordIdentifier(jobRecord)}`, copy, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  find(id: number): Observable<EntityResponseType> {
    return this.http
      .get<RestJobRecord>(`${this.resourceUrl}/${id}`, { observe: 'response' })
      .pipe(map(res => this.convertResponseFromServer(res)));
  }

  query(req?: any): Observable<EntityArrayResponseType> {
    const options = createRequestOption(req);
    return this.http
      .get<RestJobRecord[]>(this.resourceUrl, { params: options, observe: 'response' })
      .pipe(map(res => this.convertResponseArrayFromServer(res)));
  }

  delete(id: number): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}/${id}`, { observe: 'response' });
  }

  // ADAPTED
  deleteAll(): Observable<HttpResponse<{}>> {
    return this.http.delete(`${this.resourceUrl}-delete-all`, { observe: 'response' });
  }

  getJobRecordIdentifier(jobRecord: Pick<IJobRecord, 'id'>): number {
    return jobRecord.id;
  }

  compareJobRecord(o1: Pick<IJobRecord, 'id'> | null, o2: Pick<IJobRecord, 'id'> | null): boolean {
    return o1 && o2 ? this.getJobRecordIdentifier(o1) === this.getJobRecordIdentifier(o2) : o1 === o2;
  }

  addJobRecordToCollectionIfMissing<Type extends Pick<IJobRecord, 'id'>>(
    jobRecordCollection: Type[],
    ...jobRecordsToCheck: (Type | null | undefined)[]
  ): Type[] {
    const jobRecords: Type[] = jobRecordsToCheck.filter(isPresent);
    if (jobRecords.length > 0) {
      const jobRecordCollectionIdentifiers = jobRecordCollection.map(jobRecordItem => this.getJobRecordIdentifier(jobRecordItem)!);
      const jobRecordsToAdd = jobRecords.filter(jobRecordItem => {
        const jobRecordIdentifier = this.getJobRecordIdentifier(jobRecordItem);
        if (jobRecordCollectionIdentifiers.includes(jobRecordIdentifier)) {
          return false;
        }
        jobRecordCollectionIdentifiers.push(jobRecordIdentifier);
        return true;
      });
      return [...jobRecordsToAdd, ...jobRecordCollection];
    }
    return jobRecordCollection;
  }

  protected convertDateFromClient<T extends IJobRecord | NewJobRecord | PartialUpdateJobRecord>(jobRecord: T): RestOf<T> {
    return {
      ...jobRecord,
      jobAcceptedTimestamp: jobRecord.jobAcceptedTimestamp?.toJSON() ?? null,
      lastEventTimestamp: jobRecord.lastEventTimestamp?.toJSON() ?? null,
      lastRecordUpdateTimestamp: jobRecord.lastRecordUpdateTimestamp?.toJSON() ?? null,
    };
  }

  protected convertDateFromServer(restJobRecord: RestJobRecord): IJobRecord {
    return {
      ...restJobRecord,
      jobAcceptedTimestamp: restJobRecord.jobAcceptedTimestamp ? dayjs(restJobRecord.jobAcceptedTimestamp) : undefined,
      lastEventTimestamp: restJobRecord.lastEventTimestamp ? dayjs(restJobRecord.lastEventTimestamp) : undefined,
      lastRecordUpdateTimestamp: restJobRecord.lastRecordUpdateTimestamp ? dayjs(restJobRecord.lastRecordUpdateTimestamp) : undefined,
    };
  }

  protected convertResponseFromServer(res: HttpResponse<RestJobRecord>): HttpResponse<IJobRecord> {
    return res.clone({
      body: res.body ? this.convertDateFromServer(res.body) : null,
    });
  }

  protected convertResponseArrayFromServer(res: HttpResponse<RestJobRecord[]>): HttpResponse<IJobRecord[]> {
    return res.clone({
      body: res.body ? res.body.map(item => this.convertDateFromServer(item)) : null,
    });
  }
}
