import { Component } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { IProcess } from '../process.model';
import { ProcessService } from '../service/process.service';

@Component({
  templateUrl: './process-delete-dialog.component.html',
})
export class ProcessDeleteDialogComponent {
  process?: IProcess;

  constructor(protected processService: ProcessService, protected activeModal: NgbActiveModal) {}

  cancel(): void {
    this.activeModal.dismiss();
  }

  confirmDelete(id: string): void {
    this.processService.delete(id).subscribe(() => {
      this.activeModal.close('deleted');
    });
  }
}
