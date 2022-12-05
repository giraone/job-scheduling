import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { ActivationEnum } from 'app/entities/enumerations/activation-enum.model';
import { IProcess, Process } from '../process.model';

import { ProcessService } from './process.service';

describe('Process Service', () => {
  let service: ProcessService;
  let httpMock: HttpTestingController;
  let elemDefault: IProcess;
  let expectedResult: IProcess | IProcess[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(ProcessService);
    httpMock = TestBed.inject(HttpTestingController);

    elemDefault = {
      id: '000',
      name: 'AAAAAAA',
      activation: ActivationEnum.ACTIVE,
    };
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = Object.assign({}, elemDefault);

      service.find(123).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(elemDefault);
    });

    it('should create a Process', () => {
      const returnedFromService = Object.assign(
        {
          id: 0,
        },
        elemDefault
      );

      const expected = Object.assign({}, returnedFromService);

      service.create(new Process()).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Process', () => {
      const returnedFromService = Object.assign(
        {
          id: '001',
          name: 'BBBBBB',
          activation: 'BBBBBB',
        },
        elemDefault
      );

      const expected = Object.assign({}, returnedFromService);

      service.update(expected).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Process', () => {
      const patchObject = Object.assign(
        {
          name: 'BBBBBB',
          activation: 'BBBBBB',
        },
        new Process()
      );

      const returnedFromService = Object.assign(patchObject, elemDefault);

      const expected = Object.assign({}, returnedFromService);

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Process', () => {
      const returnedFromService = Object.assign(
        {
          id: '001',
          name: 'BBBBBB',
          activation: 'BBBBBB',
        },
        elemDefault
      );

      const expected = Object.assign({}, returnedFromService);

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toContainEqual(expected);
    });

    it('should delete a Process', () => {
      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult);
    });

    describe('addProcessToCollectionIfMissing', () => {
      it('should add a Process to an empty array', () => {
        const process: IProcess = { id: 123 };
        expectedResult = service.addProcessToCollectionIfMissing([], process);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(process);
      });

      it('should not add a Process to an array that contains it', () => {
        const process: IProcess = { id: 123 };
        const processCollection: IProcess[] = [
          {
            ...process,
          },
          { id: 456 },
        ];
        expectedResult = service.addProcessToCollectionIfMissing(processCollection, process);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Process to an array that doesn't contain it", () => {
        const process: IProcess = { id: 123 };
        const processCollection: IProcess[] = [{ id: 456 }];
        expectedResult = service.addProcessToCollectionIfMissing(processCollection, process);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(process);
      });

      it('should add only unique Process to an array', () => {
        const processArray: IProcess[] = [{ id: 123 }, { id: 456 }, { id: 68714 }];
        const processCollection: IProcess[] = [{ id: 123 }];
        expectedResult = service.addProcessToCollectionIfMissing(processCollection, ...processArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const process: IProcess = { id: 123 };
        const process2: IProcess = { id: 456 };
        expectedResult = service.addProcessToCollectionIfMissing([], process, process2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(process);
        expect(expectedResult).toContain(process2);
      });

      it('should accept null and undefined values', () => {
        const process: IProcess = { id: 123 };
        expectedResult = service.addProcessToCollectionIfMissing([], null, process, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(process);
      });

      it('should return initial array if no Process is added', () => {
        const processCollection: IProcess[] = [{ id: 123 }];
        expectedResult = service.addProcessToCollectionIfMissing(processCollection, undefined, null);
        expect(expectedResult).toEqual(processCollection);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
