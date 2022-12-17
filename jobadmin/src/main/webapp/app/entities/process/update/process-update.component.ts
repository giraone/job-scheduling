import { Component, OnInit } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { IProcess, Process } from '../process.model';
import { ProcessService } from '../service/process.service';
import { ActivationEnum } from 'app/entities/enumerations/activation-enum.model';

@Component({
  selector: 'jhi-process-update',
  templateUrl: './process-update.component.html',
})
export class ProcessUpdateComponent implements OnInit {
  isSaving = false;
  activationEnumValues = Object.keys(ActivationEnum);

  editForm = this.fb.group({
    id: [],
    key: [null, [Validators.required]],
    name: [null, [Validators.required]],
    activation: [null, [Validators.required]],
    agentKey: [],
    bucketKeyIfPaused: [],
  });

  constructor(protected processService: ProcessService, protected activatedRoute: ActivatedRoute, protected fb: FormBuilder) {}

  ngOnInit(): void {
    this.activatedRoute.data.subscribe(({ process }) => {
      this.updateForm(process);
    });
  }

  previousState(): void {
    window.history.back();
  }

  save(): void {
    this.isSaving = true;
    const process = this.createFromForm();
    if (process.id !== undefined) {
      this.subscribeToSaveResponse(this.processService.update(process));
    } else {
      this.subscribeToSaveResponse(this.processService.create(process));
    }
  }

  protected subscribeToSaveResponse(result: Observable<HttpResponse<IProcess>>): void {
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

  protected updateForm(process: IProcess): void {
    this.editForm.patchValue({
      id: process.id,
      key: process.key,
      name: process.name,
      activation: process.activation,
      agentKey: process.agentKey,
      bucketKeyIfPaused: process.bucketKeyIfPaused,
    });
  }

  protected createFromForm(): IProcess {
    return {
      ...new Process(),
      id: this.editForm.get(['id'])!.value,
      key: this.editForm.get(['key'])!.value,
      name: this.editForm.get(['name'])!.value,
      activation: this.editForm.get(['activation'])!.value,
      agentKey: this.editForm.get(['agentKey'])!.value,
      bucketKeyIfPaused: this.editForm.get(['bucketKeyIfPaused'])!.value,
    };
  }
}
