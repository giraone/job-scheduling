import dayjs from 'dayjs/esm';
import { IProcess } from 'app/entities/process/process.model';
import { JobStatusEnum } from 'app/entities/enumerations/job-status-enum.model';

export interface IJobRecord {
  id: number;
  jobAcceptedTimestamp?: dayjs.Dayjs | null;
  lastEventTimestamp?: dayjs.Dayjs | null;
  lastRecordUpdateTimestamp?: dayjs.Dayjs | null;
  status?: JobStatusEnum | null;
  pausedBucketKey?: string | null;
  process?: Pick<IProcess, 'id'> | null;
}

export type NewJobRecord = Omit<IJobRecord, 'id'> & { id: null };
