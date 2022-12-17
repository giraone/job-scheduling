import { Injectable } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

import { IProcess, NewProcess } from '../process.model';

/**
 * A partial Type with required key is used as form input.
 */
type PartialWithRequiredKeyOf<T extends { id: unknown }> = Partial<Omit<T, 'id'>> & { id: T['id'] };

/**
 * Type for createFormGroup and resetForm argument.
 * It accepts IProcess for edit and NewProcessFormGroupInput for create.
 */
type ProcessFormGroupInput = IProcess | PartialWithRequiredKeyOf<NewProcess>;

type ProcessFormDefaults = Pick<NewProcess, 'id'>;

type ProcessFormGroupContent = {
  id: FormControl<IProcess['id'] | NewProcess['id']>;
  key: FormControl<IProcess['key']>;
  name: FormControl<IProcess['name']>;
  activation: FormControl<IProcess['activation']>;
  agentKey: FormControl<IProcess['agentKey']>;
  bucketKeyIfPaused: FormControl<IProcess['bucketKeyIfPaused']>;
};

export type ProcessFormGroup = FormGroup<ProcessFormGroupContent>;

@Injectable({ providedIn: 'root' })
export class ProcessFormService {
  createProcessFormGroup(process: ProcessFormGroupInput = { id: null }): ProcessFormGroup {
    const processRawValue = {
      ...this.getFormDefaults(),
      ...process,
    };
    return new FormGroup<ProcessFormGroupContent>({
      id: new FormControl(
        { value: processRawValue.id, disabled: true },
        {
          nonNullable: true,
          validators: [Validators.required],
        }
      ),
      key: new FormControl(processRawValue.key, {
        validators: [Validators.required],
      }),
      name: new FormControl(processRawValue.name, {
        validators: [Validators.required],
      }),
      activation: new FormControl(processRawValue.activation, {
        validators: [Validators.required],
      }),
      agentKey: new FormControl(processRawValue.agentKey),
      bucketKeyIfPaused: new FormControl(processRawValue.bucketKeyIfPaused),
    });
  }

  getProcess(form: ProcessFormGroup): IProcess | NewProcess {
    return form.getRawValue() as IProcess | NewProcess;
  }

  resetForm(form: ProcessFormGroup, process: ProcessFormGroupInput): void {
    const processRawValue = { ...this.getFormDefaults(), ...process };
    form.reset(
      {
        ...processRawValue,
        id: { value: processRawValue.id, disabled: true },
      } as any /* cast to workaround https://github.com/angular/angular/issues/46458 */
    );
  }

  private getFormDefaults(): ProcessFormDefaults {
    return {
      id: null,
    };
  }
}
