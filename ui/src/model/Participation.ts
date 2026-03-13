import type { User } from "./User";

export interface Participation {
    user: User;
    schemaId: string;
    authorities: AuthorityType[];
}

export type AuthorityType = 'READ_SCHEME' | 'MODIFY_SCHEME' | 'INVITE_USERS' | 'VERSION' | 'ALL';