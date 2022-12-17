import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { IProcess } from '../process.model';
import { sampleWithRequiredData, sampleWithNewData, sampleWithPartialData, sampleWithFullData } from '../process.test-samples';

import { ProcessService } from './process.service';

const requireRestSample: IProcess = {
  ...sampleWithRequiredData,
};

describe('Process Service', () => {
  let service: ProcessService;
  let httpMock: HttpTestingController;
  let expectedResult: IProcess | IProcess[] | boolean | null;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    expectedResult = null;
    service = TestBed.inject(ProcessService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  describe('Service methods', () => {
    it('should find an element', () => {
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.find(123).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should create a Process', () => {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const process = { ...sampleWithNewData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.create(process).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'POST' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should update a Process', () => {
      const process = { ...sampleWithRequiredData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.update(process).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PUT' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should partial update a Process', () => {
      const patchObject = { ...sampleWithPartialData };
      const returnedFromService = { ...requireRestSample };
      const expected = { ...sampleWithRequiredData };

      service.partialUpdate(patchObject).subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'PATCH' });
      req.flush(returnedFromService);
      expect(expectedResult).toMatchObject(expected);
    });

    it('should return a list of Process', () => {
      const returnedFromService = { ...requireRestSample };

      const expected = { ...sampleWithRequiredData };

      service.query().subscribe(resp => (expectedResult = resp.body));

      const req = httpMock.expectOne({ method: 'GET' });
      req.flush([returnedFromService]);
      httpMock.verify();
      expect(expectedResult).toMatchObject([expected]);
    });

    it('should delete a Process', () => {
      const expected = true;

      service.delete(123).subscribe(resp => (expectedResult = resp.ok));

      const req = httpMock.expectOne({ method: 'DELETE' });
      req.flush({ status: 200 });
      expect(expectedResult).toBe(expected);
    });

    describe('addProcessToCollectionIfMissing', () => {
      it('should add a Process to an empty array', () => {
        const process: IProcess = sampleWithRequiredData;
        expectedResult = service.addProcessToCollectionIfMissing([], process);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(process);
      });

      it('should not add a Process to an array that contains it', () => {
        const process: IProcess = sampleWithRequiredData;
        const processCollection: IProcess[] = [
          {
            ...process,
          },
          sampleWithPartialData,
        ];
        expectedResult = service.addProcessToCollectionIfMissing(processCollection, process);
        expect(expectedResult).toHaveLength(2);
      });

      it("should add a Process to an array that doesn't contain it", () => {
        const process: IProcess = sampleWithRequiredData;
        const processCollection: IProcess[] = [sampleWithPartialData];
        expectedResult = service.addProcessToCollectionIfMissing(processCollection, process);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(process);
      });

      it('should add only unique Process to an array', () => {
        const processArray: IProcess[] = [sampleWithRequiredData, sampleWithPartialData, sampleWithFullData];
        const processCollection: IProcess[] = [sampleWithRequiredData];
        expectedResult = service.addProcessToCollectionIfMissing(processCollection, ...processArray);
        expect(expectedResult).toHaveLength(3);
      });

      it('should accept varargs', () => {
        const process: IProcess = sampleWithRequiredData;
        const process2: IProcess = sampleWithPartialData;
        expectedResult = service.addProcessToCollectionIfMissing([], process, process2);
        expect(expectedResult).toHaveLength(2);
        expect(expectedResult).toContain(process);
        expect(expectedResult).toContain(process2);
      });

      it('should accept null and undefined values', () => {
        const process: IProcess = sampleWithRequiredData;
        expectedResult = service.addProcessToCollectionIfMissing([], null, process, undefined);
        expect(expectedResult).toHaveLength(1);
        expect(expectedResult).toContain(process);
      });

      it('should return initial array if no Process is added', () => {
        const processCollection: IProcess[] = [sampleWithRequiredData];
        expectedResult = service.addProcessToCollectionIfMissing(processCollection, undefined, null);
        expect(expectedResult).toEqual(processCollection);
      });
    });

    describe('compareProcess', () => {
      it('Should return true if both entities are null', () => {
        const entity1 = null;
        const entity2 = null;

        const compareResult = service.compareProcess(entity1, entity2);

        expect(compareResult).toEqual(true);
      });

      it('Should return false if one entity is null', () => {
        const entity1 = { id: 123 };
        const entity2 = null;

        const compareResult1 = service.compareProcess(entity1, entity2);
        const compareResult2 = service.compareProcess(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey differs', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 456 };

        const compareResult1 = service.compareProcess(entity1, entity2);
        const compareResult2 = service.compareProcess(entity2, entity1);

        expect(compareResult1).toEqual(false);
        expect(compareResult2).toEqual(false);
      });

      it('Should return false if primaryKey matches', () => {
        const entity1 = { id: 123 };
        const entity2 = { id: 123 };

        const compareResult1 = service.compareProcess(entity1, entity2);
        const compareResult2 = service.compareProcess(entity2, entity1);

        expect(compareResult1).toEqual(true);
        expect(compareResult2).toEqual(true);
      });
    });
  });

  afterEach(() => {
    httpMock.verify();
  });
});
