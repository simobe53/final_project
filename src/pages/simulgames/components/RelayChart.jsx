import { useMemo } from 'react';
import {
  Chart,
  YAxis,
  XAxis,
  Legend,
  Tooltip,
  Credits
} from '@highcharts/react';
import { Column } from '@highcharts/react/series';
import './RelayBox.scss'

export default function RelayChart({ data, result }) {
    const series = useMemo(() => {
        if (!data) return [];
        return Object.keys(data).reduce((acc, curr) => {
            acc.push({ name: curr, data: data[curr] * 100, color: result === curr ? 'var(--point-color)' : 'var(--point-dim-color)' });
            return acc;
        }, []).sort((a, b) => b.data - a.data).slice(0, 6);
    }, [data, result])

    const resultPos = useMemo(() => series.findIndex(({ name }) => name === result), [series, result])

    const xLabelFormatter = (d) => d.pos === resultPos ? `<b style='color: var(--point-color)'>${d.value}</b>` : d.value;

    return (
        <div className="border-radius-20 overflow-hidden m-2">
            <Chart options={{ chart: { height: 200 }  }}>
                <Credits enabled={false} />
                <YAxis min={0} max={series[0].data} title={false} labels={{ enabled: false }} />
                <XAxis categories={series.map(({ name }) => name)} title={false} crosshair={true} labels={{ formatter: xLabelFormatter }} />
                <Legend enabled={false} />
                <Column.Series 
                    data={series.map(({ data, color }, idx) => ({ y: data, color, dataLabels: { className: idx === resultPos ? 'chart-result' : "" } }))} 
                    dataLabels={{
                        enabled: true,
                        format: '{point.y:.2f}%'
                    }}
                />
                <Tooltip format="<div class='text-center'><b>[AI 분석확률] <br/> {point.key} : <b class='point'>{point.y:.2f}%</b></b></div>" />
            </Chart>
        </div>
    );
}