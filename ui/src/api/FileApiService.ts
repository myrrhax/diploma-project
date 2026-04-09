import { AbstractApiService } from "./AbstractApiService";
import $api from "./AxiosClient";

class FileApiService extends AbstractApiService {
    async downloadFile(fileId: string, fallbackName: string = 'downloaded_file') {
        try {
            const response = await $api.get(`/files/${fileId}`, { responseType: 'blob' });

            let filename = fallbackName;
            const disposition = response.headers['content-disposition'];
            
            if (disposition) {
                if (disposition.toLowerCase().includes("filename*=utf-8''")) {
                    const match = disposition.match(/filename\*=utf-8''([^;]+)/i);
                    if (match && match[1]) filename = decodeURIComponent(match[1].replace(/['"]/g, ''));
                } else if (disposition.includes('filename=')) {
                    const match = disposition.match(/filename=(["']?)([^"';]+)\1/i);
                    if (match && match[2]) filename = decodeURIComponent(match[2]);
                }
            }

            const contentType = response.headers['content-type'] || 'application/octet-stream';
            const blob = new Blob([response.data], { type: contentType });
            const downloadUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            
            link.href = downloadUrl;
            link.download = filename; 
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