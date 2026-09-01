import React, { useState, useEffect, useCallback } from 'react';
import AnalyticsCard from '../components/AnalyticsCard';
import DailyVolumeChart from '../components/DailyVolumeChart';
import PerformanceTable from '../components/PerformanceTable';
import {
  getAnalyticsOverview,
  getDailyVolume,
  getBusiestCounter,
  getPeakHour,
  getCounterPerformance,
} from '../services/api';

export default function Analytics() {
  const [overview, setOverview] = useState(null);
  const [dailyVolume, setDailyVolume] = useState([]);
  const [busiestCounter, setBusiestCounter] = useState(null);
  const [peakHour, setPeakHour] = useState(null);
  const [performanceData, setPerformanceData] = useState([]);
  const [selectedDate, setSelectedDate] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingPerf, setIsLoadingPerf] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const loadAllAnalytics = useCallback(async () => {
    try {
      setIsLoading(true);
      setErrorMessage('');

      const [overviewRes, dailyRes, busiestRes, peakRes, perfRes] = await Promise.all([
        getAnalyticsOverview().catch(() => null),
        getDailyVolume().catch(() => []),
        getBusiestCounter().catch(() => null),
        getPeakHour().catch(() => null),
        getCounterPerformance(selectedDate || null).catch(() => []),
      ]);

      setOverview(overviewRes);
      setDailyVolume(dailyRes || []);
      setBusiestCounter(busiestRes || overviewRes?.busiestCounter || null);
      setPeakHour(peakRes || overviewRes?.peakHour || null);
      setPerformanceData(perfRes || overviewRes?.counterPerformance || []);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to load queue analytics');
    } finally {
      setIsLoading(false);
    }
  }, [selectedDate]);

  useEffect(() => {
    loadAllAnalytics();
  }, [loadAllAnalytics]);

  const handleDateFilterChange = async (dateStr) => {
    setSelectedDate(dateStr);
    try {
      setIsLoadingPerf(true);
      const perf = await getCounterPerformance(dateStr || null);
      setPerformanceData(perf || []);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to filter counter performance');
    } finally {
      setIsLoadingPerf(false);
    }
  };

  const handleClearDate = async () => {
    setSelectedDate('');
    try {
      setIsLoadingPerf(true);
      const perf = await getCounterPerformance(null);
      setPerformanceData(perf || []);
    } catch (err) {
      setErrorMessage(err.message || 'Failed to reset date filter');
    } finally {
      setIsLoadingPerf(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <h1 className="page-title">Campus Queue Analytics</h1>
          <p className="page-subtitle">
            Real-time aggregate queue metrics computed directly in PostgreSQL (Zero Java memory loops).
          </p>
        </div>

        <button className="btn btn-outline" onClick={loadAllAnalytics} disabled={isLoading}>
          {isLoading ? <span className="spinner spinner-dark" /> : '🔄 Refresh Metrics'}
        </button>
      </div>

      {errorMessage && (
        <div className="alert alert-error">
          <span>⚠️</span>
          <div>{errorMessage}</div>
        </div>
      )}

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '60px' }}>
          <span className="spinner spinner-dark" />
          <div style={{ marginTop: '12px', color: 'var(--slate-500)' }}>
            Aggregating database analytics...
          </div>
        </div>
      ) : (
        <>
          {/* Summary KPI Cards */}
          <div className="analytics-grid">
            <AnalyticsCard
              label="Total Tickets Issued"
              value={overview?.totalTicketsOverall ?? 0}
              subtext="All-time tokens generated"
              icon="🎫"
            />
            <AnalyticsCard
              label="Completed Successfully"
              value={overview?.totalCompletedOverall ?? 0}
              subtext="Students served"
              icon="✅"
              accentColor="var(--emerald-600)"
            />
            <AnalyticsCard
              label="Busiest Service Desk"
              value={busiestCounter?.counterName || 'None'}
              subtext={`${busiestCounter?.handledTickets ?? 0} tickets served`}
              icon="🏆"
              accentColor="var(--primary-700)"
            />
            <AnalyticsCard
              label="Peak Queue Traffic"
              value={peakHour?.formattedHour || 'N/A'}
              subtext={`${peakHour?.ticketCount ?? 0} tickets generated`}
              icon="⏰"
              accentColor="var(--amber-600)"
            />
            <AnalyticsCard
              label="Avg. Waiting Time"
              value={`${overview?.averageWaitMinutesOverall ?? 0} min`}
              subtext="(called_at - created_at)"
              icon="⏳"
            />
            <AnalyticsCard
              label="Avg. Handling Time"
              value={`${overview?.averageServiceMinutesOverall ?? 0} min`}
              subtext="(completed_at - called_at)"
              icon="⏱"
            />
          </div>

          {/* Daily Ticket Volume Chart */}
          <DailyVolumeChart dailyData={dailyVolume} />

          {/* Detailed Counter Breakdown Table */}
          <PerformanceTable
            performanceData={performanceData}
            selectedDate={selectedDate}
            onDateChange={handleDateFilterChange}
            onClearDate={handleClearDate}
            isLoading={isLoadingPerf}
          />
        </>
      )}
    </div>
  );
}
