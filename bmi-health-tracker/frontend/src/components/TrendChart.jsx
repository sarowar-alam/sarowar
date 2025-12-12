import React,{useEffect,useState}from'react';import{Line}from'react-chartjs-2';
import api from'../api';import{Chart as C,CategoryScale,LinearScale,PointElement,LineElement,Title,Tooltip,Legend}from'chart.js';
C.register(CategoryScale,LinearScale,PointElement,LineElement,Title,Tooltip,Legend);
export default function TC(){const[d,sd]=useState(null);useEffect(()=>{api.get('/measurements/trends')
.then(r=>{const rows=r.data.rows;sd({labels:rows.map(x=>x.day),datasets:[{label:'Avg BMI',data:rows.map(x=>x.avg_bmi)}]});});},[]);
return d?<Line data={d}/>:<div>Loading...</div>}