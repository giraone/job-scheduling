import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize, map } from 'rxjs/operators';

import { JobRecordFormService, JobRecordFormGroup } from './job-record-form.service';
import { IJobRecord } from '../job-record.model';
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
  jobRecord: IJobRecord | null = null;
  jobStatusEnumValues = Object.keys(JobStatusEnum);

  processesSharedCollection: IProcess[] = [];

  editForm: JobRecordFormGroup = this.jobRecordFormService.createJobRecordFormGroup();

  constructor(
    protected jobRecordService: JobRecordService,
    protected jobRecordFormService: JobRecordFormService,
    protected processService: ProcessService,
    protected activatedRoute: ActivatedRoute
  ) {}

  compareProcess = (o1: IProcess | null, o2: IProcess | null): boolean => this.processService.compareProcess(o1, o2);

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ jobRecord }) => {
      this.jobRecord = jobRecord;
      if (jobRecord) {
        this.updateForm(jobRecord);
      }

      this.loadRelationshipsOptions();
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const jobRecord = this.jobRecordFormService.getJobRecord(this.editForm);
    if (jobRecord.id !== null) {
      this.subscribeToSaveResponse(this.jobRecordService.update(jobRecord));
    } else {
      this.subscribeToSaveResponse(this.jobRecordService.create(jobRecord));
    }
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
    this.jobRecord = jobRecord;
    this.jobRecordFormService.resetForm(this.editForm, jobRecord);

    this.processesSharedCollection = this.processService.addProcessToCollectionIfMissing<IProcess>(
      this.processesSharedCollection,
      jobRecord.process
    );
  }

  protected loadRelationshipsOptions(): void {
    this.processService
      .query()
      .pipe(map((res: HttpResponse<IProcess[]>) => res.body ?? []))
      .pipe(
        map((processes: IProcess[]) => this.processService.addProcessToCollectionIfMissing<IProcess>(processes, this.jobRecord?.process))
      )
      .subscribe((processes: IProcess[]) => (this.processesSharedCollection = processes));
  }
}
