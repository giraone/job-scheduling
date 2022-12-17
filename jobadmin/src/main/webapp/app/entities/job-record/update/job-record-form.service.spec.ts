import { TestBed } from '@angular/core/testing';

import { sampleWithRequiredData, sampleWithNewData } from '../job-record.test-samples';

import { JobRecordFormService } from './job-record-form.service';

describe('JobRecord Form Service', () => {
  let service: JobRecordFormService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(JobRecordFormService);
  });

  describe('Service methods', () => {
    describe('createJobRecordFormGroup', () => {
      it('should create a new form with FormControl', () => {
        const formGroup = service.createJobRecordFormGroup();

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            jobAcceptedTimestamp: expect.any(Object),
            lastEventTimestamp: expect.any(Object),
            lastRecordUpdateTimestamp: expect.any(Object),
            status: expect.any(Object),
            pausedBucketKey: expect.any(Object),
            process: expect.any(Object),
          })
        );
      });

      it('passing IJobRecord should create a new form with FormGroup', () => {
        const formGroup = service.createJobRecordFormGroup(sampleWithRequiredData);

        expect(formGroup.controls).toEqual(
          expect.objectContaining({
            id: expect.any(Object),
            jobAcceptedTimestamp: expect.any(Object),
            lastEventTimestamp: expect.any(Object),
            lastRecordUpdateTimestamp: expect.any(Object),
            status: expect.any(Object),
            pausedBucketKey: expect.any(Object),
            process: expect.any(Object),
          })
        );
      });
    });

    describe('getJobRecord', () => {
      it('should return NewJobRecord for default JobRecord initial value', () => {
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const formGroup = service.createJobRecordFormGroup(sampleWithNewData);

        const jobRecord = service.getJobRecord(formGroup) as any;

        expect(jobRecord).toMatchObject(sampleWithNewData);
      });

      it('should return NewJobRecord for empty JobRecord initial value', () => {
        const formGroup = service.createJobRecordFormGroup();

        const jobRecord = service.getJobRecord(formGroup) as any;

        expect(jobRecord).toMatchObject({});
      });

      it('should return IJobRecord', () => {
        const formGroup = service.createJobRecordFormGroup(sampleWithRequiredData);

        const jobRecord = service.getJobRecord(formGroup) as any;

        expect(jobRecord).toMatchObject(sampleWithRequiredData);
      });
    });

    describe('resetForm', () => {
      it('passing IJobRecord should not enable id FormControl', () => {
        const formGroup = service.createJobRecordFormGroup();
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, sampleWithRequiredData);

        expect(formGroup.controls.id.disabled).toBe(true);
      });

      it('passing NewJobRecord should disable id FormControl', () => {
        const formGroup = service.createJobRecordFormGroup(sampleWithRequiredData);
        expect(formGroup.controls.id.disabled).toBe(true);

        service.resetForm(formGroup, { id: null });

        expect(formGroup.controls.id.disabled).toBe(true);
      });
    });
  });
});
