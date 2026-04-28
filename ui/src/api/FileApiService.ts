import { AbstractApiService } from "./AbstractApiService";
import $api from "./AxiosClient";

class FileApiService extends AbstractApiService {
    async downloadFile(fileId: string, fallbackName: string = 'downloaded_file') {
        try {
            const response = await $api.get(`/files/${fileId}`, { responseType: 'blob' });

            const contentType = response.headers['content-type'] || 'application/octet-stream';
            const blob = new Blob([response.data], { type: contentType });
            const downloadUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            
            link.href = downloadUrl;
            link.download = fallbackName; 
            document.body.appendChild(link);
            link.click();
            
            link.remove();
            window.URL.revokeObjectURL(downloadUrl);
        } catch (error) {
            console.error("Ошибка при скачивании файла:", error);
        }
    }
}

export const filesApi = new FileApiService(); 