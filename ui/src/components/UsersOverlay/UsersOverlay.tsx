import type { User } from "@/model/User";
import profilePic from '@/assets/user.png';
import './UsersOverlay.css'

interface UsersOverlayProps {
    isUsersOpen: boolean;
    users: User[];
    closeCallback: ((arg: boolean) => void);
}

export const UsersOverlay = ({isUsersOpen, users, closeCallback}: UsersOverlayProps) => {
    return (
        <aside className={`users-overlay ${!isUsersOpen ? 'collapsed' : ''}`}>
            <div className="users-header" onClick={() => closeCallback(!isUsersOpen)}>
              <span>В сети ({users.length})</span>
              <button className="toggle-users-btn">
                {isUsersOpen ? 
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 9l6 6 6-6"/></svg> : 
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 15l-6-6-6 6"/></svg>
                }
              </button>
            </div>
            {isUsersOpen && (
              <div className="users-list">
                {users.map(user => (
                  <div key={user.id} className="user-item">
                    <img src={profilePic} alt="user" className="user-avatar" />
                    <span className="user-email">{user.email}</span>
                  </div>
                ))}
              </div>
            )}  
        </aside>
    )
}