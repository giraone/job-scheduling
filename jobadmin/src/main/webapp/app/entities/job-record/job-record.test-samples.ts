import dayjs from 'dayjs/esm';

import { JobStatusEnum } from 'app/entities/enumerations/job-status-enum.model';

import { IJobRecord, NewJobRecord } from './job-record.model';

export const sampleWithRequiredData: IJobRecord = {
  id: 87521,
  jobAcceptedTimestamp: dayjs('2022-11-30T16:01'),
  lastEventTimestamp: dayjs('2022-11-30T16:25'),
  lastRecordUpdateTimestamp: dayjs('2022-12-01T12:44'),
  status: JobStatusEnum['DELIVERED'],
};

export const sampleWithPartialData: IJobRecord = {
  id: 64594,
  jobAcceptedTimestamp: dayjs('2022-12-01T05:07'),
  lastEventTimestamp: dayjs('2022-12-01T03:52'),
  lastRecordUpdateTimestamp: dayjs('2022-11-30T20:51'),
  status: JobStatusEnum['SCHEDULED'],
};

export const sampleWithFullData: IJobRecord = {
  id: 34600,
  jobAcceptedTimestamp: dayjs('2022-12-01T13:19'),
  lastEventTimestamp: dayjs('2022-11-30T15:04'),
  lastRecordUpdateTimestamp: dayjs('2022-12-01T08:17'),
  status: JobStatusEnum['NOTIFIED'],
  pausedBucketKey: 'incentivize context-sensitive red',
};

export const sampleWithNewData: NewJobRecord = {
  jobAcceptedTimestamp: dayjs('2022-11-30T22:15'),
  lastEventTimestamp: dayjs('2022-12-01T05:44'),
  lastRecordUpdateTimestamp: dayjs('2022-12-01T01:44'),
  status: JobStatusEnum['PAUSED'],
  id: null,
};

Object.freeze(sampleWithNewData);
Object.freeze(sampleWithRequiredData);
Object.freeze(sampleWithPartialData);
Object.freeze(sampleWithFullData);
