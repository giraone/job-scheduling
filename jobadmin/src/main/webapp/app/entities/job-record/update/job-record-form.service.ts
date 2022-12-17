import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import dayjs from 'dayjs/esm';
import { DATE_TIME_FORMAT } from 'app/config/input.constants';
import { IJobRecord, NewJobRecord } from '../job-record.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IJobRecord for edit and NewJobRecordFormGroupInput for create.
 */
type JobRecordFormGroupInput = IJobRecord | PartialWithRequiredKeyOf<NewJobRecord>;

/**
 * Type that converts some properties for forms.
 */
type FormValueOf<T extends IJobRecord | NewJobRecord> = Omit<
  T,
  'jobAcceptedTimestamp' | 'lastEventTimestamp' | 'lastRecordUpdateTimestamp'
> & {
  jobAcceptedTimestamp?: string | null;
  lastEventTimestamp?: string | null;
  lastRecordUpdateTimestamp?: string | null;
};

type JobRecordFormRawValue = FormValueOf<IJobRecord>;

type NewJobRecordFormRawValue = FormValueOf<NewJobRecord>;

type JobRecordFormDefaults = Pick<NewJobRecord, 'id' | 'jobAcceptedTimestamp' | 'lastEventTimestamp' | 'lastRecordUpdateTimestamp'>;

type JobRecordFormGroupContent = {
  id: FormControl<JobRecordFormRawValue['id'] | NewJobRecord['id']>;
  jobAcceptedTimestamp: FormControl<JobRecordFormRawValue['jobAcceptedTimestamp']>;
  lastEventTimestamp: FormControl<JobRecordFormRawValue['lastEventTimestamp']>;
  lastRecordUpdateTimestamp: FormControl<JobRecordFormRawValue['lastRecordUpdateTimestamp']>;
  status: FormControl<JobRecordFormRawValue['status']>;
  pausedBucketKey: FormControl<JobRecordFormRawValue['pausedBucketKey']>;
  process: FormControl<JobRecordFormRawValue['process']>;
};

export type JobRecordFormGroup = FormGroup<JobRecordFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class JobRecordFormService {
  createJobRecordFormGroup(jobRecord: JobRecordFormGroupInput = { id: null }): JobRecordFormGroup {
    const jobRecordRawValue = this.convertJobRecordToJobRecordRawValue({
      ...this.getFormDefaults(),
      ...jobRecord,
    });
    return new FormGroup<JobRecordFormGroupContent>({
      id: new FormControl(
        { value: jobRecordRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),
      jobAcceptedTimestamp: new FormControl(jobRecordRawValue.jobAcceptedTimestamp, {
        validators: [Validators.required],
      }),
      lastEventTimestamp: new FormControl(jobRecordRawValue.lastEventTimestamp, {
        validators: [Validators.required],
      }),
      lastRecordUpdateTimestamp: new FormControl(jobRecordRawValue.lastRecordUpdateTimestamp, {
        validators: [Validators.required],
      }),
      status: new FormControl(jobRecordRawValue.status, {
        validators: [Validators.required],
      }),
      pausedBucketKey: new FormControl(jobRecordRawValue.pausedBucketKey),
      process: new FormControl(jobRecordRawValue.process, {
        validators: [Validators.required],
      }),
    });
  }

  getJobRecord(form: JobRecordFormGroup): IJobRecord | NewJobRecord {
    return this.convertJobRecordRawValueToJobRecord(form.getRawValue() as JobRecordFormRawValue | NewJobRecordFormRawValue);
  }

  resetForm(form: JobRecordFormGroup, jobRecord: JobRecordFormGroupInput): void {
    const jobRecordRawValue = this.convertJobRecordToJobRecordRawValue({ ...this.getFormDefaults(), ...jobRecord });
    form.reset(
      {
        ...jobRecordRawValue,
        id: { value: jobRecordRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */
    );
  }

  private getFormDefaults(): JobRecordFormDefaults {
    const currentTime = dayjs();

    return {
      id: null,
      jobAcceptedTimestamp: currentTime,
      lastEventTimestamp: currentTime,
      lastRecordUpdateTimestamp: currentTime,
    };
  }

  private convertJobRecordRawValueToJobRecord(rawJobRecord: JobRecordFormRawValue | NewJobRecordFormRawValue): IJobRecord | NewJobRecord {
    return {
      ...rawJobRecord,
      jobAcceptedTimestamp: dayjs(rawJobRecord.jobAcceptedTimestamp, DATE_TIME_FORMAT),
      lastEventTimestamp: dayjs(rawJobRecord.lastEventTimestamp, DATE_TIME_FORMAT),
      lastRecordUpdateTimestamp: dayjs(rawJobRecord.lastRecordUpdateTimestamp, DATE_TIME_FORMAT),
    };
  }

  private convertJobRecordToJobRecordRawValue(
    jobRecord: IJobRecord | (Partial<NewJobRecord> & JobRecordFormDefaults)
  ): JobRecordFormRawValue | PartialWithRequiredKeyOf<NewJobRecordFormRawValue> {
    return {
      ...jobRecord,
      jobAcceptedTimestamp: jobRecord.jobAcceptedTimestamp ? jobRecord.jobAcceptedTimestamp.format(DATE_TIME_FORMAT) : undefined,
      lastEventTimestamp: jobRecord.lastEventTimestamp ? jobRecord.lastEventTimestamp.format(DATE_TIME_FORMAT) : undefined,
      lastRecordUpdateTimestamp: jobRecord.lastRecordUpdateTimestamp
        ? jobRecord.lastRecordUpdateTimestamp.format(DATE_TIME_FORMAT)
        : undefined,
    };
  }
}
