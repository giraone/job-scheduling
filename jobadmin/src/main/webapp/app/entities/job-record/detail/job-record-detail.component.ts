import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IJobRecord } from '../job-record.model';

@Component({
  selector: 'jhi-job-record-detail',
  templateUrl: './job-record-detail.component.html',
})
export class JobRecordDetailComponent implements OnInit {
  jobRecord: IJobRecord | null = null;

  constructor(protected activatedRoute: ActivatedRoute) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ jobRecord }) => {
      this.jobRecord = jobRecord;
    });
  }

  previousState(): void {
    window.history.back();
  }
}
