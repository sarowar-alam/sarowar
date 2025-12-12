import React,{useEffect,useState}from'react';import MeasurementForm from'./components/MeasurementForm';
import TrendChart from'./components/TrendChart';import api from'./api';
export default function App(){const[rows,setRows]=useState([]);const load=async()=>{const r=await api.get('/measurements');setRows(r.data.rows);}
useEffect(()=>{load()},[]);return(<div><h1>BMI Tracker</h1><MeasurementForm onSaved={load}/>
<h2>Recent</h2><ul>{rows.map(r=><li key={r.id}>{r.created_at} BMI:{r.bmi}</li>)}</ul><TrendChart/></div>);}