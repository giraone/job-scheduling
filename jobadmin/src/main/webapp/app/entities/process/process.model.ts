import { ActivationEnum } from 'app/entities/enumerations/activation-enum.model';

export interface IProcess {
  id?: number;
  key?: string;
  name?: string;
  activation?: ActivationEnum;
  agentKey?: string | null;
  bucketKeyIfPaused?: string | null;
}

export class Process implements IProcess {
  constructor(
    public id?: number,
    public key?: string,
    public name?: string,
    public activation?: ActivationEnum,
    public agentKey?: string | null,
    public bucketKeyIfPaused?: string | null
  ) {}
}

export function getProcessIdentifier(process: IProcess): number | undefined {
  return process.id;
}
