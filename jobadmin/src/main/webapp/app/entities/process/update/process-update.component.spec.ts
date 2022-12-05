import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, Subject, from } from 'rxjs';

import { ProcessService } from '../service/process.service';
import { IProcess, Process } from '../process.model';

import { ProcessUpdateComponent } from './process-update.component';

describe('Process Management Update Component', () => {
  let comp: ProcessUpdateComponent;
  let fixture: ComponentFixture<ProcessUpdateComponent>;
  let activatedRoute: ActivatedRoute;
  let processService: ProcessService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule.withRoutes([])],
      declarations: [ProcessUpdateComponent],
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
      .overrideTemplate(ProcessUpdateComponent, '')
      .compileComponents();

    fixture = TestBed.createComponent(ProcessUpdateComponent);
    activatedRoute = TestBed.inject(ActivatedRoute);
    processService = TestBed.inject(ProcessService);

    comp = fixture.componentInstance;
  });

  describe('ngOnInit', () => {
    it('Should update editForm', () => {
      const process: IProcess = { id: 456 };

      activatedRoute.data = of({ process });
      comp.ngOnInit();

      expect(comp.editForm.value).toEqual(expect.objectContaining(process));
    });
  });

  describe('save', () => {
    it('Should call update service on save for existing entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<Process>>();
      const process = { id: 123 };
      jest.spyOn(processService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ process });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: process }));
      saveSubject.complete();

      // THEN
      expect(comp.previousState).toHaveBeenCalled();
      expect(processService.update).toHaveBeenCalledWith(process);
      expect(comp.isSaving).toEqual(false);
    });

    it('Should call create service on save for new entity', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<Process>>();
      const process = new Process();
      jest.spyOn(processService, 'create').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ process });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.next(new HttpResponse({ body: process }));
      saveSubject.complete();

      // THEN
      expect(processService.create).toHaveBeenCalledWith(process);
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).toHaveBeenCalled();
    });

    it('Should set isSaving to false on error', () => {
      // GIVEN
      const saveSubject = new Subject<HttpResponse<Process>>();
      const process = { id: 123 };
      jest.spyOn(processService, 'update').mockReturnValue(saveSubject);
      jest.spyOn(comp, 'previousState');
      activatedRoute.data = of({ process });
      comp.ngOnInit();

      // WHEN
      comp.save();
      expect(comp.isSaving).toEqual(true);
      saveSubject.error('This is an error!');

      // THEN
      expect(processService.update).toHaveBeenCalledWith(process);
      expect(comp.isSaving).toEqual(false);
      expect(comp.previousState).not.toHaveBeenCalled();
    });
  });
});
