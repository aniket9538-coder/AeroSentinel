import hotspotApi from './hotspotApi';
import { HotspotOverviewResponse, HotspotCell } from '../types/hotspot';

export const hotspotService = {
  getHotspotsOverview: (cityId: string): Promise<HotspotOverviewResponse> => {
    return hotspotApi.getHotspotsByCity(cityId);
  },
  getHotspotByH3: (h3Index: string): Promise<HotspotCell> => {
    return hotspotApi.getHotspotByH3(h3Index);
  },
  // Backward compatibility alias returning cell list
  getHotspots: async (cityId: string): Promise<HotspotCell[]> => {
    const data = await hotspotApi.getHotspotsByCity(cityId);
    return data.cells || [];
  },
};

export default hotspotService;
