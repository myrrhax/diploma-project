import type { Script, ScriptType } from "@/model/SchemaTypes";
import { scriptsStore } from "@/store/ScriptsStore";
import { observer } from "mobx-react-lite";
import { useEffect, useMemo, useState } from "react";
import React from "react";
import './ScriptsModal.css';
import { erStore } from "@/store/ERStore";
import { filesApi } from "@/api/FileApiService";
import { CreateScriptModal } from "../CreateScriptModal/CreateScriptModal";

const SqlIcon = () => (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
        <polyline points="14 2 14 8 20 8"></polyline>
        <text x="7" y="16" fontSize="7" fontWeight="bold" fill="#3b82f6" stroke="none">SQL</text>
    </svg>
);

const YamlIcon = () => (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
        <polyline points="14 2 14 8 20 8"></polyline>
        <text x="6" y="16" fontSize="6" fontWeight="bold" fill="#10b981" stroke="none">YML</text>
    </svg>
);

export const ScriptsModal = observer(() => {
    const schemeId = erStore.schemaId;

    useEffect(() => {
        if (schemeId) {
            scriptsStore.loadScripts(schemeId);
        }
    }, [schemeId]);

    const groupedScripts = useMemo(() => {
        const groups: Record<string, Script[]> = {};
        scriptsStore.scripts.forEach(script => {
            const tag = script.version.tag;
            if (!tag) {
                return;
            }
            if (!groups[tag]) {
                groups[tag] = [];
            }
            groups[tag].push(script);
        });
        
        Object.keys(groups).forEach(tag => {
            groups[tag].sort((a, b) => a.type.localeCompare(b.type) || a.generatedType.localeCompare(b.generatedType));
        });

        return groups;
    }, [scriptsStore.scripts]);

    const getFileExtension = (type: ScriptType) => {
        return type === 'LIQUIBASE' ? '.yaml' : '.sql';
    };

    return (
        <div className="scripts_modal_overlay" onMouseDown={() => scriptsStore.closeModal()}>
            <div className="scripts_modal_container" onMouseDown={e => e.stopPropagation()}>
                
                <div className="scripts_modal_header">
                    <div className="scripts_header_left">
                        <h2 className="scripts_modal_title">Скрипты проекта</h2>
                        <button className="scripts_create_btn" onClick={() => scriptsStore.openCreateScriptModal()}>
                            + Создать скрипт
                        </button>
                    </div>
                    <button className="scripts_modal_close" onClick={() => scriptsStore.closeModal()}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <line x1="18" y1="6" x2="6" y2="18"></line>
                            <line x1="6" y1="6" x2="18" y2="18"></line>
                        </svg>
                    </button>
                </div>

                <div className="scripts_modal_content">
                    {Object.keys(groupedScripts).length === 0 ? (
                        <div className="scripts_empty_state">Скрипты не найдены</div>
                    ) : (
                        <table className="scripts_table">
                            <thead>
                                <tr>
                                    <th style={{ width: '50px' }}>Файл</th>
                                    <th>Имя файла (Версия)</th>
                                    <th>База / Инструмент</th>
                                    <th>Тип генерации</th>
                                    <th>Детали (Миграция от)</th>
                                </tr>
                            </thead>
                            <tbody>
                                {Object.entries(groupedScripts).map(([versionTag, scripts]) => (
                                    <React.Fragment key={versionTag}>
                                        <tr className="scripts_group_header">
                                            <td colSpan={5}>Версия: {versionTag}</td>
                                        </tr>
                                        
                                        {scripts.map(script => (
                                            <tr key={script.id} className="scripts_table_row">
                                                <td className="scripts_icon_cell">
                                                    {script.type === 'LIQUIBASE' ? <YamlIcon /> : <SqlIcon />}
                                                </td>
                                                <td>
                                                    <a 
                                                        className="scripts_download_link"
                                                        onClick={(e) => {
                                                            e.preventDefault();
                                                            const expectedName = `${versionTag}_${script.type.toLowerCase()}${getFileExtension(script.type)}`;
                                                            filesApi.downloadFile(script.scriptFileId, expectedName);
                                                        }}
                                                    >
                                                        {versionTag}_{script.type.toLowerCase()}{getFileExtension(script.type)}
                                                        <svg className="download_icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                                                            <polyline points="7 10 12 15 17 10"></polyline>
                                                            <line x1="12" y1="15" x2="12" y2="3"></line>
                                                        </svg>
                                                    </a>
                                                </td>
                                                <td>
                                                    <span className={`badge badge_${script.type.toLowerCase()}`}>
                                                        {script.type}
                                                    </span>
                                                </td>
                                                <td>
                                                    <span className={`badge badge_${script.generatedType.toLowerCase()}`}>
                                                        {script.generatedType}
                                                    </span>
                                                </td>
                                                <td className="scripts_details_cell">
                                                    {script.generatedType === 'MIGRATION' && script.fromVersion ? (
                                                        <span className="migration_text">
                                                            от <strong>{script.fromVersion.tag}</strong> к <strong>{versionTag}</strong>
                                                        </span>
                                                    ) : (
                                                        <span className="full_text">—</span>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                    </React.Fragment>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
});