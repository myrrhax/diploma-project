import type { User } from "./User";

export interface Participation {
    user: User;
    schemaId: string;
    authorities: AuthorityType[];
}

export type AuthorityType = 'READ_SCHEME' | 'MODIFY_SCHEME' | 'SNAPSHOT_VERSION' | 'DELETE_VERSIONS'
    | 'INVITE_USERS' | 'CHANGE_HEAD' | 'ALL';