import { ActivationEnum } from 'app/entities/enumerations/activation-enum.model';

export interface IProcess {
  id: number;
  key?: string | null;
  name?: string | null;
  activation?: ActivationEnum | null;
  agentKey?: string | null;
  bucketKeyIfPaused?: string | null;
}

export type NewProcess = Omit<IProcess, 'id'> & { id: null };
