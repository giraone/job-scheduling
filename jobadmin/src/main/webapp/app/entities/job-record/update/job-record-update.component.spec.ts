import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, Subject, from } from 'rxjs';

import { JobRecordFormService } from './job-record-form.service';
import { JobRecordService } from '../service/job-record.service';
import { IJobRecord } from '../job-record.model';
import { IProcess } from 'app/entities/process/process.model';
import { ProcessService } from 'app/entities/process/service/process.service';

import { JobRecordUpdateComponent } from './job-record-update.component';

describe('JobRecord Management Update Component', () => {
  let comp: JobRecordUpdateComponent;
  let fixture: ComponentFixture<JobRecordUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let jobRecordFormService: JobRecordFormService;
  let jobRecordService: JobRecordService;
  let processService: ProcessService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule.withRoutes([])],
      declarations: [JobRecordUpdateComponent],
      providers: [
        FormBuilder,
        {
          provide: ActivatedRoute,
          useValue: {
            params: from([{}]),
          },
        },
      ],
    })
      .overrideTemplate(JobRecordUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(JobRecordUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    jobRecordFormService = TestBed.inject(JobRecordFormService);
    jobRecordService = TestBed.inject(JobRecordService);
    processService = TestBed.inject(ProcessService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should call Process query and add missing value', () => {
      const jobRecord: IJobRecord = { id: 456 };
      const process: IProcess = { id: 11438 };
      jobRecord.process = process;

      const processCollection: IProcess[] = [{ id: 2271 }];
      jest.spyOn(processService, 'query').mockReturnValue(of(new HttpResponse({ body: processCollection })));
      const additionalProcesses = [process];
      const expectedCollection: IProcess[] = [...additionalProcesses, ...processCollection];
      jest.spyOn(processService, 'addProcessToCollectionIfMissing').mockReturnValue(expectedCollection);

      activatedRoute.data = of({ jobRecord });
      comp.ngOnInit();

      expect(processService.query).toHaveBeenCalled();
      expect(processService.addProcessToCollectionIfMissing).toHaveBeenCalledWith(
        processCollection,
        ...additionalProcesses.map(expect.objectContaining)
      );
      expect(comp.processesSharedCollection).toEqual(expectedCollection);
    });

    it('Should update editForm', () => {
      const jobRecord: IJobRecord = { id: 456 };
      const process: IProcess = { id: 54237 };
      jobRecord.process = process;

      activatedRoute.data = of({ jobRecord });
      comp.ngOnInit();

      expect(comp.processesSharedCollection).toContain(process);
      expect(comp.jobRecord).toEqual(jobRecord);
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IJobRecord>>();
      const jobRecord = { id: 123 };
      jest.spyOn(jobRecordFormService, 'getJobRecord').mockReturnValue(jobRecord);
      jest.spyOn(jobRecordService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ jobRecord });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: jobRecord }));
      saveSubject.complete();

      // THEN
      expect(jobRecordFormService.getJobRecord).toHaveBeenCalled();
      expect(comp.previousState).toHaveBeenCalled();
      expect(jobRecordService.update).toHaveBeenCalledWith(expect.objectContaining(jobRecord));
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IJobRecord>>();
      const jobRecord = { id: 123 };
      jest.spyOn(jobRecordFormService, 'getJobRecord').mockReturnValue({ id: null });
      jest.spyOn(jobRecordService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ jobRecord: null });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: jobRecord }));
      saveSubject.complete();

      // THEN
      expect(jobRecordFormService.getJobRecord).toHaveBeenCalled();
      expect(jobRecordService.create).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<IJobRecord>>();
      const jobRecord = { id: 123 };
      jest.spyOn(jobRecordService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ jobRecord });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(jobRecordService.update).toHaveBeenCalled();
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });

  describe('Compare relationships', () => {
    describe('compareProcess', () => {
      it('Should forward to processService', () => {
        const entity = { id: 123 };
        const entity2 = { id: 456 };
        jest.spyOn(processService, 'compareProcess');
        comp.compareProcess(entity, entity2);
        expect(processService.compareProcess).toHaveBeenCalledWith(entity, entity2);
      });
    });
  });
});
