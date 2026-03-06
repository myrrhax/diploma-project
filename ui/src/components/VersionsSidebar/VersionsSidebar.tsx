import './VersionSidebar.css'
import { versionsStore } from '@/store/VersionsStore';
import { observer } from 'mobx-react-lite';

interface VersionsSidebarProps {
    isOpen: boolean;
    changeVisibleCallback: ((open: boolean) => void);
}

export const VersionsSidebar = observer(({isOpen, changeVisibleCallback}: VersionsSidebarProps) => {
	const { isLoading, versions } = versionsStore;

    return (
		<aside className={`versions-sidebar ${isOpen ? 'open' : ''}`}>
			<div className="sidebar-toggle" onClick={() => changeVisibleCallback(!isOpen)}>
				<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d={isOpen ? "M15 18l-6-6 6-6" : "M9 18l6-6-6-6"}/></svg>
			</div>
			<div className="sidebar-content">
				<h3>История версий</h3>
				<div className="version-list">
					{isLoading ? (<div>Загрузка...</div>) : (
						versions.map(v => (
							<div key={v.versionId} className={`version-item ${v.isWorkingCopy ? 'version-working': ''} ${v.isInitial ? 'version-initial' : ''}`}>
								<span className="version-name">{v.tag ?? 'Рабочая версия'}</span>
								{v.versionedAt ? (
									<span className="version-date">{v.versionedAt?.toString()}</span>
								) : null}
							</div>
						))
					)}
				</div>
            </div>
        </aside>
    )
})