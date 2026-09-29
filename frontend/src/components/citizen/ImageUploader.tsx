import React, { useRef, useState } from 'react';
import { UploadCloud, Camera, Image as ImageIcon, X, Check, AlertCircle } from 'lucide-react';
import { Button } from '../common/Button';

interface ImageUploaderProps {
  onImageSelected: (file: File) => void;
  onClearImage?: () => void;
}

const MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export const ImageUploader: React.FC<ImageUploaderProps> = ({
  onImageSelected,
  onClearImage,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);

  const validateAndProcessFile = (file: File) => {
    setValidationError(null);

    // File type validation
    if (!ALLOWED_TYPES.includes(file.type)) {
      setValidationError('Unsupported file format. Please upload JPG, PNG, or WebP imagery.');
      return;
    }

    // File size validation (15MB max)
    if (file.size > MAX_FILE_SIZE) {
      setValidationError('Image size exceeds 15MB limit. Please attach a compressed photo.');
      return;
    }

    const objectUrl = URL.createObjectURL(file);
    setPreview(objectUrl);
    onImageSelected(file);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      validateAndProcessFile(e.target.files[0]);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      validateAndProcessFile(e.dataTransfer.files[0]);
    }
  };

  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    setPreview(null);
    setValidationError(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
    if (cameraInputRef.current) cameraInputRef.current.value = '';
    if (onClearImage) onClearImage();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept="image/jpeg,image/png,image/webp"
        style={{ display: 'none' }}
        data-testid="citizen-file-input"
        aria-label="Upload citizen observation photo"
      />
      <input
        type="file"
        ref={cameraInputRef}
        onChange={handleFileChange}
        accept="image/*"
        capture="environment"
        style={{ display: 'none' }}
        data-testid="citizen-camera-input"
        aria-label="Capture citizen observation photo with camera"
      />

      {/* Validation Error Feedback */}
      {validationError && (
        <div
          role="alert"
          style={{
            padding: '0.75rem 1rem',
            borderRadius: '8px',
            background: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            color: 'var(--accent-red, #ef4444)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            fontSize: '0.8rem',
          }}
        >
          <AlertCircle size={16} style={{ flexShrink: 0 }} />
          <span>{validationError}</span>
        </div>
      )}

      {preview ? (
        <div
          style={{
            position: 'relative',
            borderRadius: '12px',
            overflow: 'hidden',
            border: '1px solid var(--border-medium, #cbd5e1)',
            background: 'var(--bg-surface-elevated, #ffffff)',
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
              aria-label="Remove photo"
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
              color: 'var(--text-secondary, #94a3b8)',
            }}
          >
            <span style={{ color: 'var(--accent-teal, #10b981)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.3rem' }}>
              <Check size={14} /> Photo Attached
            </span>
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--brand-primary, #38bdf8)',
                cursor: 'pointer',
                fontSize: '0.75rem',
                fontWeight: 600,
              }}
              aria-label="Replace attached photo"
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
            border: isDragging ? '2px dashed var(--brand-primary, #38bdf8)' : '2px dashed var(--border-medium, #cbd5e1)',
            borderRadius: '14px',
            padding: '2.5rem 1.5rem',
            textAlign: 'center',
            cursor: 'pointer',
            background: isDragging ? 'rgba(56, 189, 248, 0.06)' : 'var(--bg-surface-elevated, #f8fafc)',
            transition: 'all 0.2s ease',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.75rem',
          }}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              fileInputRef.current?.click();
            }
          }}
          aria-label="Upload photo area. Click or drag and drop to attach image."
        >
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              background: 'var(--brand-surface, rgba(56, 189, 248, 0.1))',
              border: '1px solid var(--brand-border, rgba(56, 189, 248, 0.25))',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'var(--brand-primary, #0284c7)',
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
              aria-label="Open camera to take photo"
            >
              <Camera size={14} style={{ marginRight: '0.4rem' }} /> Take Photo
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};
