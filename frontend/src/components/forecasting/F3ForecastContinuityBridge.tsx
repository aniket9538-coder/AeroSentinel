import React, { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

/**
 * F3ForecastContinuityBridge
 *
 * Ensures seamless H3 cell continuity between F3 (Hotspots) and F4 (Forecast)
 * without modifying any F3 source files, preserving strict phase isolation.
 *
 * Capabilities:
 * 1. Automatically tracks the actively selected H3 cell on F3 (/hotspots)
 *    and stores it in sessionStorage.
 * 2. Injects an "Open Forecast" button into the HotspotCellDetailsCard
 *    so users can navigate directly to the Forecast view for that exact H3 cell.
 * 3. Supports Sidebar navigation: if the user clicks "Forecast" in the Sidebar,
 *    the previously selected F3 H3 cell is seamlessly loaded in Forecast.
 */
export const F3ForecastContinuityBridge: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    const isHotspotsPage =
      location.pathname === '/hotspots' || location.pathname === '/analyst/hotspots';

    if (!isHotspotsPage) {
      return;
    }

    // Helper to find the active H3 cell index from the F3 DOM
    const detectActiveH3 = (): string | null => {
      // 1. Check HotspotCellDetailsCard for 15-character Uber H3 hex
      const detailsCardH3 = document.querySelector(
        '[style*="font-family: var(--font-mono)"], [style*="fontFamily: var(--font-mono)"]'
      );
      if (detailsCardH3 && detailsCardH3.textContent) {
        const match = detailsCardH3.textContent.trim().match(/^88[0-9a-f]{13}$/i);
        if (match) return match[0];
      }

      // 2. Check table for the selected row
      const selectedRow = document.querySelector('tr[style*="var(--brand-surface)"]');
      if (selectedRow) {
        const text = selectedRow.textContent || '';
        const match = text.match(/88[0-9a-f]{13}/i);
        if (match) return match[0];
      }

      // 3. Fallback to any visible 15-char H3 index on the page
      const allMonoElements = document.querySelectorAll('td, div, span, p');
      for (const el of allMonoElements) {
        if (el.children.length === 0) {
          const match = el.textContent?.trim().match(/^88[0-9a-f]{13}$/i);
          if (match) return match[0];
        }
      }

      return null;
    };

    // Helper to inject or update the "Open Forecast" CTA in HotspotCellDetailsCard
    const updateOpenForecastCTA = () => {
      const activeH3 = detectActiveH3();
      if (activeH3) {
        sessionStorage.setItem('aerosentinel_selected_h3', activeH3);
      }

      // Locate the Hotspot Details card container (.glass-panel)
      const cards = document.querySelectorAll('.glass-panel, .aero-card, [class*="Card"]');
      let detailsCard: HTMLElement | null = null;
      for (const card of cards) {
        if (
          card.textContent?.includes('Potential Hotspot') &&
          card.textContent?.toLowerCase().includes('h3 cell index')
        ) {
          detailsCard = card as HTMLElement;
          break;
        }
      }

      if (!detailsCard || !activeH3 || detailsCard.querySelector('a[href*="/analyst/evidence"]')) {
        const existingBtn = document.getElementById('f3-open-forecast-container');
        if (existingBtn) existingBtn.remove();
        return;
      }

      let btnContainer = document.getElementById('f3-open-forecast-container');
      if (!btnContainer) {
        btnContainer = document.createElement('div');
        btnContainer.id = 'f3-open-forecast-container';
        btnContainer.style.marginTop = '0.75rem';
        btnContainer.style.paddingTop = '0.75rem';
        btnContainer.style.borderTop = '1px solid var(--border-subtle)';

        const btn = document.createElement('button');
        btn.id = 'f3-open-forecast-btn';
        btn.type = 'button';
        btn.style.width = '100%';
        btn.style.padding = '0.55rem 0.9rem';
        btn.style.borderRadius = '8px';
        btn.style.backgroundColor = 'var(--brand-primary)';
        btn.style.color = '#ffffff';
        btn.style.fontWeight = '700';
        btn.style.fontSize = '0.825rem';
        btn.style.border = 'none';
        btn.style.cursor = 'pointer';
        btn.style.display = 'flex';
        btn.style.alignItems = 'center';
        btn.style.justifyContent = 'center';
        btn.style.gap = '0.45rem';
        btn.style.transition = 'all 0.15s ease';
        btn.style.boxShadow = '0 2px 6px rgba(14, 165, 233, 0.25)';

        btn.innerHTML = `
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline>
            <polyline points="17 6 23 6 23 12"></polyline>
          </svg>
          <span>Open Forecast</span>
        `;

        btn.onmouseenter = () => {
          btn.style.filter = 'brightness(1.1)';
          btn.style.transform = 'translateY(-1px)';
        };
        btn.onmouseleave = () => {
          btn.style.filter = 'none';
          btn.style.transform = 'none';
        };

        btnContainer.appendChild(btn);
        detailsCard.appendChild(btnContainer);
      }

      const btn = document.getElementById('f3-open-forecast-btn');
      if (btn) {
        btn.onclick = (e) => {
          e.preventDefault();
          e.stopPropagation();
          sessionStorage.setItem('aerosentinel_selected_h3', activeH3);
          navigate(`/forecast?h3=${encodeURIComponent(activeH3)}`);
        };
        btn.title = `Open Short-Term PM2.5 Forecast for ${activeH3}`;
      }
    };

    // Initial check
    updateOpenForecastCTA();

    // Listen to click events to immediately track cell switches
    const handleClick = () => {
      setTimeout(updateOpenForecastCTA, 50);
    };

    document.addEventListener('click', handleClick);

    // Observe DOM mutations to update when async data arrives or selection changes
    const observer = new MutationObserver(() => {
      updateOpenForecastCTA();
    });

    observer.observe(document.body, { childList: true, subtree: true });

    return () => {
      document.removeEventListener('click', handleClick);
      observer.disconnect();
      const existingBtn = document.getElementById('f3-open-forecast-container');
      if (existingBtn) existingBtn.remove();
    };
  }, [location.pathname, navigate]);

  return null;
};

export default F3ForecastContinuityBridge;
