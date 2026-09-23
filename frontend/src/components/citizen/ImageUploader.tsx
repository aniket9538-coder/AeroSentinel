import React, { useRef, useState } from 'react';
import { UploadCloud, Image as ImageIcon } from 'lucide-react';

interface ImageUploaderProps {
  onImageSelected: (file: File) => void;
}

export const ImageUploader: React.FC<ImageUploaderProps> = ({ onImageSelected }) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setPreview(URL.createObjectURL(file));
      onImageSelected(file);
    }
  };

  return (
    <div
      onClick={() => fileInputRef.current?.click()}
      style={{
        border: '2px dashed rgba(255, 255, 255, 0.15)',
        borderRadius: '8px',
        padding: '1.5rem',
        textAlign: 'center',
        cursor: 'pointer',
        background: 'rgba(255, 255, 255, 0.02)',
      }}
    >
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept="image/jpeg,image/png,image/webp"
        style={{ display: 'none' }}
      />
      {preview ? (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
          <img
            src={preview}
            alt="Preview"
            style={{ maxHeight: '160px', borderRadius: '6px', objectFit: 'cover', marginBottom: '0.5rem' }}
          />
          <span style={{ fontSize: '0.75rem', color: '#9ca3af' }}>Click to change image</span>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem' }}>
          <UploadCloud size={32} color="#06b6d4" />
          <div style={{ fontSize: '0.875rem', fontWeight: 500, color: '#f3f4f6' }}>Upload Emission Photo</div>
          <div style={{ fontSize: '0.75rem', color: '#6b7280' }}>JPEG, PNG, or WebP (Max 10MB)</div>
        </div>
      )}
    </div>
  );
};
