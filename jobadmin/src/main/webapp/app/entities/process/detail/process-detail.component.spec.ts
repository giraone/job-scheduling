import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ProcessDetailComponent } from './process-detail.component';

describe('Process Management Detail Component', () => {
  let comp: ProcessDetailComponent;
  let fixture: ComponentFixture<ProcessDetailComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ProcessDetailComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { data: of({ process: { id: 123 } }) },
        },
      ],
    })
      .overrideTemplate(ProcessDetailComponent, '')
      .compileComponents();
    fixture = TestBed.createComponent(ProcessDetailComponent);
    comp = fixture.componentInstance;
  });

  describe('OnInit', () => {
    it('Should load process on init', () => {
      // WHEN
      comp.ngOnInit();

      // THEN
      expect(comp.process).toEqual(expect.objectContaining({ id: 123 }));
    });
  });
});
