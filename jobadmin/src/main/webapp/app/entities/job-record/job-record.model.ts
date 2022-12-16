import dayjs from 'dayjs/esm';
import { IProcess } from 'app/entities/process/process.model';
import { JobStatusEnum } from 'app/entities/enumerations/job-status-enum.model';

export interface IJobRecord {
  id?: number;
  jobAcceptedTimestamp?: dayjs.Dayjs;
  lastEventTimestamp?: dayjs.Dayjs;
  lastRecordUpdateTimestamp?: dayjs.Dayjs;
  status?: JobStatusEnum;
  pausedBucketKey?: string | null;
  process?: IProcess;
}

export class JobRecord implements IJobRecord {
  constructor(
    public id?: number,
    public jobAcceptedTimestamp?: dayjs.Dayjs,
    public lastEventTimestamp?: dayjs.Dayjs,
    public lastRecordUpdateTimestamp?: dayjs.Dayjs,
    public status?: JobStatusEnum,
    public pausedBucketKey?: string | null,
    public process?: IProcess
  ) {}
}

export function getJobRecordIdentifier(jobRecord: IJobRecord): number | undefined {
  return jobRecord.id;
}
