const express=require('express');const router=express.Router();
const db=require('./db');const {calculateMetrics}=require('./calculations');
router.post('/measurements',async(req,res)=>{try{
 const {weightKg,heightCm,age,sex,activity}=req.body;
 const m=calculateMetrics({weightKg,heightCm,age,sex,activity});
 const q=`INSERT INTO measurements (weight_kg,height_cm,age,sex,activity_level,bmi,bmi_category,bmr,daily_calories,created_at)
 VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,now()) RETURNING *`;
 const v=[weightKg,heightCm,age,sex,activity,m.bmi,m.bmiCategory,m.bmr,m.dailyCalories];
 const r=await db.query(q,v);res.json({measurement:r.rows[0]});}catch(e){res.status(500).json({error:'server error'})}});
router.get('/measurements',async(req,res)=>{const r=await db.query('SELECT * FROM measurements ORDER BY created_at DESC');res.json({rows:r.rows});});
router.get('/measurements/trends',async(req,res)=>{const q=`SELECT date_trunc('day',created_at)day,avg(bmi)avg_bmi FROM measurements
 WHERE created_at>now()-interval '30 days' GROUP BY day ORDER BY day`;const r=await db.query(q);res.json({rows:r.rows});});
module.exports=router;