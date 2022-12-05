import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { IJobRecord } from '../job-record.model';
import { JobRecordService } from '../service/job-record.service';

@Component({
  templateUrl: './job-record-delete-dialog.component.html',
})
export class JobRecordDeleteDialogComponent {
  jobRecord?: IJobRecord;

  constructor(protected jobRecordService: JobRecordService, protected activeModal: NgbActiveModal) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: number): void {
    this.jobRecordService.delete(id).subscribe(() => {
      this.activeModal.close('deleted');
    });
  }
}
