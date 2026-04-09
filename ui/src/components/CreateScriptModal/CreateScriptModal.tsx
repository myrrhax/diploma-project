import type { ScriptType, GenType } from "@/model/SchemaTypes";
import { scriptsStore } from "@/store/ScriptsStore";
import { versionsStore } from "@/store/VersionsStore";
import { observer } from "mobx-react-lite";
import { useEffect, useState } from "react";
import './CreateScriptModal.css'

export const CreateScriptModal = observer(() => {
    const versions = versionsStore.versions;
    const canMigrate = versions.length > 1;

    const [targetVersionId, setTargetVersionId] = useState(versions[0]?.versionId);
    const [genType, setGenType] = useState<GenType>('FULL');
    const [scriptType, setScriptType] = useState<ScriptType>('POSTGRES');
    const [sourceVersionId, setSourceVersionId] = useState<number | null>(null);

    if (!scriptsStore.isCreateModalOpen) {
        return;
    }

    useEffect(() => {
        if (!canMigrate && genType === 'MIGRATION') {
            setGenType('FULL');
        }
    }, [canMigrate, genType]);

    useEffect(() => {
        if (genType === 'MIGRATION') {
            const availableSources = versions.filter(v => v.versionId !== targetVersionId);
            if (availableSources.length > 0 && (!sourceVersionId || sourceVersionId === targetVersionId)) {
                setSourceVersionId(availableSources[0].versionId);
            }
        }
    }, [genType, targetVersionId, versions, sourceVersionId]);

    const handleSubmit = () => {
        if (!targetVersionId) return;
        if (genType === 'MIGRATION' && !sourceVersionId) return;

        scriptsStore.generateScript(
            targetVersionId,
            scriptType,
            genType,
            sourceVersionId
        );        
    };

    return (
        <div className="create_script_overlay" onMouseDown={() => scriptsStore.closeCreateScriptsModal()}>
            <div className="create_script_content" onMouseDown={e => e.stopPropagation()}>
                <h3 className="create_script_title">Создание нового скрипта</h3>

                <div className="create_form_group">
                    <label className="create_label">Целевая версия</label>
                    <select 
                        className="create_select" 
                        value={targetVersionId} 
                        onChange={e => setTargetVersionId(Number(e.target.value))}
                    >
                        {versions.map(v => (
                            <option key={v.versionId} value={v.versionId}>{v.tag}</option>
                        ))}
                    </select>
                </div>

                <div className="create_form_group">
                    <label className="create_label">Тип генерации</label>
                    <div className="create_radio_group">
                        <label className={`create_radio_label ${genType === 'FULL' ? 'active' : ''}`}>
                            <input 
                                type="radio" 
                                name="genType" 
                                value="FULL" 
                                checked={genType === 'FULL'} 
                                onChange={() => setGenType('FULL')} 
                            />
                            Полный (FULL)
                        </label>
                        <label className={`create_radio_label ${genType === 'MIGRATION' ? 'active' : ''} ${!canMigrate ? 'disabled' : ''}`}>
                            <input 
                                type="radio" 
                                name="genType" 
                                value="MIGRATION" 
                                checked={genType === 'MIGRATION'} 
                                disabled={!canMigrate}
                                onChange={() => setGenType('MIGRATION')} 
                            />
                            Миграция (MIGRATION)
                        </label>
                    </div>
                </div>

                {genType === 'MIGRATION' && sourceVersionId && (
                    <div className="create_form_group">
                        <label className="create_label">Мигрировать от версии</label>
                        <select 
                            className="create_select" 
                            value={sourceVersionId} 
                            onChange={e => setSourceVersionId(Number(e.target.value))}
                        >
                            {versions.filter(v => v.versionId !== targetVersionId).map(v => (
                                <option key={v.versionId} value={v.versionId}>{v.tag}</option>
                            ))}
                        </select>
                    </div>
                )}

                <div className="create_form_group">
                    <label className="create_label">Инструмент / База данных</label>
                    <select 
                        className="create_select" 
                        value={scriptType} 
                        onChange={e => setScriptType(e.target.value as ScriptType)}
                    >
                        <option value="POSTGRES">PostgreSQL</option>
                        <option value="MYSQL">MySQL</option>
                        <option value="LIQUIBASE">Liquibase</option>
                    </select>
                </div>

                <div className="create_script_actions">
                    <button 
                        className="create_script_btn submit" 
                        onClick={handleSubmit}
                        disabled={!targetVersionId || (genType === 'MIGRATION' && !sourceVersionId)}
                    >
                        Сгенерировать
                    </button>
                    <button className="create_script_btn cancel" onClick={() => scriptsStore.closeCreateScriptsModal()}>
                        Отмена
                    </button>
                </div>
            </div>
        </div>
    );
});