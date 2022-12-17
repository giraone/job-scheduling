import dayjs from 'dayjs/esm';

import { JobStatusEnum } from 'app/entities/enumerations/job-status-enum.model';

import { IJobRecord, NewJobRecord } from './job-record.model';

export const sampleWithRequiredData: IJobRecord = {
  id: 87521,
  jobAcceptedTimestamp: dayjs('2022-12-16T11:25'),
  lastEventTimestamp: dayjs('2022-12-16T11:50'),
  lastRecordUpdateTimestamp: dayjs('2022-12-17T08:09'),
  status: JobStatusEnum['DELIVERED'],
};

export const sampleWithPartialData: IJobRecord = {
  id: 64594,
  jobAcceptedTimestamp: dayjs('2022-12-17T00:32'),
  lastEventTimestamp: dayjs('2022-12-16T23:17'),
  lastRecordUpdateTimestamp: dayjs('2022-12-16T16:16'),
  status: JobStatusEnum['SCHEDULED'],
};

export const sampleWithFullData: IJobRecord = {
  id: 34600,
  jobAcceptedTimestamp: dayjs('2022-12-17T08:44'),
  lastEventTimestamp: dayjs('2022-12-16T10:29'),
  lastRecordUpdateTimestamp: dayjs('2022-12-17T03:42'),
  status: JobStatusEnum['NOTIFIED'],
  pausedBucketKey: 'incentivize context-sensitive red',
};

export const sampleWithNewData: NewJobRecord = {
  jobAcceptedTimestamp: dayjs('2022-12-16T17:40'),
  lastEventTimestamp: dayjs('2022-12-17T01:08'),
  lastRecordUpdateTimestamp: dayjs('2022-12-16T21:09'),
  status: JobStatusEnum['PAUSED'],
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
