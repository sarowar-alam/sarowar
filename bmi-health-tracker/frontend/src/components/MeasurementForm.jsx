import React,{useState}from'react';import api from'../api';
export default function MF({onSaved}){const[f,sf]=useState({weightKg:70,heightCm:175,age:30,sex:'male',activity:'moderate'});
const sub=async e=>{e.preventDefault();await api.post('/measurements',f);onSaved&&onSaved();};
return(<form onSubmit={sub}><input value={f.weightKg} onChange={e=>sf({...f,weightKg:+e.target.value})}/>
<input value={f.heightCm} onChange={e=>sf({...f,heightCm:+e.target.value})}/>
<button>Save</button></form>);}