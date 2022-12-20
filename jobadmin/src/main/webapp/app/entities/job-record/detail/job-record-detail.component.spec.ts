import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { JobRecordDetailComponent } from './job-record-detail.component';

describe('JobRecord Management Detail Component', () => {
  let comp: JobRecordDetailComponent;
  let fixture: ComponentFixture<JobRecordDetailComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [JobRecordDetailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: of({ jobRecord: { id: '123' } }) },
        },
      ],
    })
      .overrideTemplate(JobRecordDetailComponent, '')
      .compileComponents();
    fixture = TestBed.createComponent(JobRecordDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load jobRecord on init', () => {
      // WHEN
      comp.ngOnInit();

      // THEN
      expect(comp.jobRecord).toEqual(expect.objectContaining({ id: '123' }));
    });
  });
});
