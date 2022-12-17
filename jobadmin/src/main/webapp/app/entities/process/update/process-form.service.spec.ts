import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../process.test-samples';

import { ProcessFormService } from './process-form.service';

describe('Process Form Service', () => {
  let service: ProcessFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ProcessFormService);
  });

  describe('Service methods', () => {
    describe('createProcessFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createProcessFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            key: expect.any(Object),
            name: expect.any(Object),
            activation: expect.any(Object),
            agentKey: expect.any(Object),
            bucketKeyIfPaused: expect.any(Object),
          })
        );
      });

      it('passing IProcess should create a new form with FormGroup', () => {
        const formGroup = service.createProcessFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            key: expect.any(Object),
            name: expect.any(Object),
            activation: expect.any(Object),
            agentKey: expect.any(Object),
            bucketKeyIfPaused: expect.any(Object),
          })
        );
      });
    });

    describe('getProcess', () => {
      it('should return NewProcess for default Process initial value', () => {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const formGroup = service.createProcessFormGroup(sampleWithNewData);

        const process = service.getProcess(formGroup) as any;

        expect(process).toMatchObject(sampleWithNewData);
      });

      it('should return NewProcess for empty Process initial value', () => {
        const formGroup = service.createProcessFormGroup();

        const process = service.getProcess(formGroup) as any;

        expect(process).toMatchObject({});
      });

      it('should return IProcess', () => {
        const formGroup = service.createProcessFormGroup(sampleWithRequiredData);

        const process = service.getProcess(formGroup) as any;

        expect(process).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IProcess should not enable id FormControl', () => {
        const formGroup = service.createProcessFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewProcess should disable id FormControl', () => {
        const formGroup = service.createProcessFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
