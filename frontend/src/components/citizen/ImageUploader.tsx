import React, { useRef, useState } from 'react';
import { UploadCloud, Camera, Image as ImageIcon, X, Check } from 'lucide-react';
import { Button } from '../common/Button';

interface ImageUploaderProps {
  onImageSelected: (file: File) => void;
  onClearImage?: () => void;
}

export const ImageUploader: React.FC<ImageUploaderProps> = ({
  onImageSelected,
  onClearImage,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);

  const processFile = (file: File) => {
    const objectUrl = URL.createObjectURL(file);
    setPreview(objectUrl);
    onImageSelected(file);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      processFile(e.target.files[0]);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      processFile(e.dataTransfer.files[0]);
    }
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    setPreview(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
    if (cameraInputRef.current) cameraInputRef.current.value = '';
    if (onClearImage) onClearImage();
  };

  return (
    <div>
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept="image/jpeg,image/png,image/webp"
        style={{ display: 'none' }}
      />
      <input
        type="file"
        ref={cameraInputRef}
        onChange={handleFileChange}
        accept="image/*"
        capture="environment"
        style={{ display: 'none' }}
      />

      {preview ? (
        <div
          style={{
            position: 'relative',
            borderRadius: '12px',
            overflow: 'hidden',
            border: '1px solid var(--border-medium)',
            background: 'var(--bg-surface-elevated)',
          }}
        >
          <img
            src={preview}
            alt="Environmental observation preview"
            style={{
              width: '100%',
              maxHeight: '260px',
              objectFit: 'cover',
              display: 'block',
            }}
          />
          <div
            style={{
              position: 'absolute',
              top: '10px',
              right: '10px',
              display: 'flex',
              gap: '0.5rem',
            }}
          >
            <button
              type="button"
              onClick={handleClear}
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                background: 'rgba(15, 23, 42, 0.75)',
                border: '1px solid rgba(255, 255, 255, 0.2)',
                color: '#ffffff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                cursor: 'pointer',
              }}
              title="Remove photo"
            >
              <X size={16} />
            </button>
          </div>
          <div
            style={{
              padding: '0.6rem 1rem',
              background: 'rgba(15, 23, 42, 0.85)',
              backdropFilter: 'blur(8px)',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              fontSize: '0.8rem',
              color: 'var(--text-secondary)',
            }}
          >
            <span style={{ color: 'var(--accent-teal)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
              <Check size={14} /> Photo Attached
            </span>
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--brand-primary)',
                cursor: 'pointer',
                fontSize: '0.75rem',
              }}
            >
              Replace Photo
            </button>
          </div>
        </div>
      ) : (
        <div
          onDragOver={(e) => {
            e.preventDefault();
            setIsDragging(true);
          }}
          onDragLeave={() => setIsDragging(false)}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          style={{
            border: isDragging ? '2px dashed var(--brand-primary)' : '2px dashed var(--border-medium)',
            borderRadius: '14px',
            padding: '2.5rem 1.5rem',
            textAlign: 'center',
            cursor: 'pointer',
            background: isDragging ? 'rgba(56, 189, 248, 0.06)' : 'var(--bg-surface-elevated)',
            transition: 'all 0.2s ease',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.75rem',
          }}
        >
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              background: 'var(--brand-surface)',
              border: '1px solid var(--brand-border)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--brand-primary)',
            }}
          >
            <UploadCloud size={28} />
          </div>

          <div>
            <div style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              Upload Environmental Photo
            </div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
              Drag and drop emission image here, or browse files
            </div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: '0.2rem' }}>
              JPG, PNG, or WebP (Max 15MB)
            </div>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '0.5rem' }}>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={(e) => {
                e.stopPropagation();
                cameraInputRef.current?.click();
              }}
            >
              <Camera size={14} style={{ marginRight: '0.4rem' }} /> Take Photo
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};
