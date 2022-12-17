import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import dayjs from 'dayjs/esm';
import { DATE_TIME_FORMAT } from 'app/config/input.constants';

import { IJobRecord, JobRecord } from '../job-record.model';
import { JobRecordService } from '../service/job-record.service';
import { IProcess } from 'app/entities/process/process.model';
import { ProcessService } from 'app/entities/process/service/process.service';
import { JobStatusEnum } from 'app/entities/enumerations/job-status-enum.model';

@Component({
  selector: 'jhi-job-record-update',
  templateUrl: './job-record-update.component.html',
})
export class JobRecordUpdateComponent implements OnInit {
  isSaving = false;
  jobStatusEnumValues = Object.keys(JobStatusEnum);

  processesSharedCollection: IProcess[] = [];

  editForm = this.fb.group({
    id: [],
    jobAcceptedTimestamp: [null, [Validators.required]],
    lastEventTimestamp: [null, [Validators.required]],
    lastRecordUpdateTimestamp: [null, [Validators.required]],
    status: [null, [Validators.required]],
    pausedBucketKey: [],
    process: [null, Validators.required],
  });

  constructor(
    protected jobRecordService: JobRecordService,
    protected processService: ProcessService,
    protected activatedRoute: ActivatedRoute,
    protected fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ jobRecord }) => {
      if (jobRecord.id === undefined) {
        const today = dayjs().startOf('day');
        jobRecord.jobAcceptedTimestamp = today;
        jobRecord.lastEventTimestamp = today;
        jobRecord.lastRecordUpdateTimestamp = today;
      }

      this.updateForm(jobRecord);

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const jobRecord = this.createFromForm();
    if (jobRecord.id !== undefined) {
      this.subscribeToSaveResponse(this.jobRecordService.update(jobRecord));
    } else {
      this.subscribeToSaveResponse(this.jobRecordService.create(jobRecord));
    }
  }

  trackProcessById(_index: number, item: IProcess): number {
    return item.id!;
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IJobRecord>>): void {
    result.pipe(finalize(() => this.onSaveFinalize())).subscribe({
      next: () => this.onSaveSuccess(),
      error: () => this.onSaveError(),
    });
  }

  protected onSaveSuccess(): void {
    this.previousState();
  }

  protected onSaveError(): void {
    // Api for inheritance.
  }

  protected onSaveFinalize(): void {
    this.isSaving = false;
  }

  protected updateForm(jobRecord: IJobRecord): void {
    this.editForm.patchValue({
      id: jobRecord.id,
      jobAcceptedTimestamp: jobRecord.jobAcceptedTimestamp ? jobRecord.jobAcceptedTimestamp.format(DATE_TIME_FORMAT) : null,
      lastEventTimestamp: jobRecord.lastEventTimestamp ? jobRecord.lastEventTimestamp.format(DATE_TIME_FORMAT) : null,
      lastRecordUpdateTimestamp: jobRecord.lastRecordUpdateTimestamp ? jobRecord.lastRecordUpdateTimestamp.format(DATE_TIME_FORMAT) : null,
      status: jobRecord.status,
      pausedBucketKey: jobRecord.pausedBucketKey,
      process: jobRecord.process,
    });

    this.processesSharedCollection = this.processService.addProcessToCollectionIfMissing(this.processesSharedCollection, jobRecord.process);
  }

  protected loadRelationshipsOptions(): void {
    this.processService
      .query()
      .pipe(map((res: HttpResponse<IProcess[]>) => res.body ?? []))
      .pipe(
        map((processes: IProcess[]) => this.processService.addProcessToCollectionIfMissing(processes, this.editForm.get('process')!.value))
      )
      .subscribe((processes: IProcess[]) => (this.processesSharedCollection = processes));
  }

  protected createFromForm(): IJobRecord {
    return {
      ...new JobRecord(),
      id: this.editForm.get(['id'])!.value,
      jobAcceptedTimestamp: this.editForm.get(['jobAcceptedTimestamp'])!.value
        ? dayjs(this.editForm.get(['jobAcceptedTimestamp'])!.value, DATE_TIME_FORMAT)
        : undefined,
      lastEventTimestamp: this.editForm.get(['lastEventTimestamp'])!.value
        ? dayjs(this.editForm.get(['lastEventTimestamp'])!.value, DATE_TIME_FORMAT)
        : undefined,
      lastRecordUpdateTimestamp: this.editForm.get(['lastRecordUpdateTimestamp'])!.value
        ? dayjs(this.editForm.get(['lastRecordUpdateTimestamp'])!.value, DATE_TIME_FORMAT)
        : undefined,
      status: this.editForm.get(['status'])!.value,
      pausedBucketKey: this.editForm.get(['pausedBucketKey'])!.value,
      process: this.editForm.get(['process'])!.value,
    };
  }
}
