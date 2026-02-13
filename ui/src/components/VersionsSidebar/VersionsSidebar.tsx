import './VersionSidebar.css'

interface VersionsSidebarProps {
    isOpen: boolean;
    changeVisibleCallback: ((open: boolean) => void);
    versions: {id: number, name: string, date: string}[]; // ToDo переделать под Version
}

export const VersionsSidebar = ({isOpen, changeVisibleCallback, versions}: VersionsSidebarProps) => {
    return (
        <aside className={`versions-sidebar ${isOpen ? 'open' : ''}`}>
          <div className="sidebar-toggle" onClick={() => changeVisibleCallback(!isOpen)}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d={isOpen ? "M15 18l-6-6 6-6" : "M9 18l6-6-6-6"}/></svg>
          </div>
          <div className="sidebar-content">
            <h3>История версий</h3>
            <div className="version-list">
              {versions.map(v => (
                <div key={v.id} className="version-item">
                  <span className="version-name">{v.name}</span>
                  <span className="version-date">{v.date}</span>
                </div>
              ))}
            </div>
          </div>
        </aside>
    )
}