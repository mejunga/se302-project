import sys
import json
import os
from datetime import datetime, timedelta
from reportlab.lib import colors
from reportlab.lib.colors import HexColor
from reportlab.lib.pagesizes import A4, landscape
from reportlab.pdfgen import canvas
from reportlab.lib.units import mm

def parse_time_to_minutes(time_str):
    try:
        h, m = map(int, time_str.split(':'))
        return h * 60 + m
    except:
        return 0

def draw_grid_schedule(c, width, height, room_name, exams, start_date_str, total_days, start_hour=8, end_hour=20):
    margin_left = 25 * mm
    margin_right = 10 * mm
    margin_top = 25 * mm
    margin_bottom = 15 * mm
    
    draw_w = width - margin_left - margin_right
    draw_h = height - margin_top - margin_bottom
    
    num_days = total_days
    col_width = draw_w / num_days

    total_minutes = (end_hour - start_hour) * 60
    px_per_min = draw_h / total_minutes

    c.setStrokeAlpha(1)
    c.setFillAlpha(1)
    c.setFont("Helvetica-Bold", 16)
    c.setFillColor(colors.black)
    c.drawString(margin_left, height - 15 * mm, f"Exam Schedule - {room_name}")
    
    c.setLineWidth(0.3)
    c.setFont("Helvetica", 8)

    for h in range(start_hour, end_hour + 1):
        minutes_from_start = (h - start_hour) * 60
        y = height - margin_top - (minutes_from_start * px_per_min)
        
        c.setDash([]) 
        c.setStrokeColor(HexColor("#e0e0e0")) 
        c.line(margin_left, y, width - margin_right, y)
        
        c.setFillColor(colors.gray)
        c.drawString(8 * mm, y - 3, f"{h:02d}:00")
        
        if h < end_hour:
            y_half = y - (30 * px_per_min)
            c.setDash([2, 2]) 
            c.setStrokeColor(HexColor("#f0f0f0")) 
            c.line(margin_left, y_half, width - margin_right, y_half)

    c.setDash([]) 
    c.setStrokeColor(HexColor("#b0b0b0")) 
    c.setLineWidth(0.5)
    
    try:
        dt_start = datetime.strptime(start_date_str, "%Y-%m-%d")
    except:
        dt_start = datetime.now()

    for i in range(num_days):
        x = margin_left + (i * col_width)
        c.line(x, height - margin_top, x, margin_bottom)
        
        current_date = dt_start + timedelta(days=i)
        date_label = current_date.strftime("%d/%m")     
        day_name_label = current_date.strftime("%A")  
        
        c.setFillColor(HexColor("#f5f5f5"))
        c.rect(x, height - margin_top, col_width, 15*mm, fill=1, stroke=0)

        c.setFillColor(HexColor("#2c3e50")) 
        font_size = 10 if num_days < 10 else 8
        c.setFont("Helvetica-Bold", font_size)
        
        w_lbl = c.stringWidth(day_name_label, "Helvetica-Bold", font_size)
        c.drawString(x + (col_width - w_lbl) / 2, height - margin_top + 10, day_name_label)
        
        c.setFillColor(colors.darkgray)
        c.setFont("Helvetica", font_size - 1)
        w_date = c.stringWidth(date_label, "Helvetica", font_size - 1)
        c.drawString(x + (col_width - w_date) / 2, height - margin_top + 2, date_label)

    c.setStrokeColor(HexColor("#b0b0b0"))
    c.line(width - margin_right, height - margin_top, width - margin_right, margin_bottom)

    BOX_COLOR = HexColor("#4a90e2") 
    BORDER_COLOR = HexColor("#357abd") 
    TEXT_COLOR = colors.black 

    for exam in exams:
        day_idx = exam['dayIndex']
        if day_idx >= num_days: continue

        try:
            t_range = exam['timeRange'] 
            start_s, end_s = t_range.split(" - ")
            start_m = parse_time_to_minutes(start_s)
            end_m = parse_time_to_minutes(end_s)
        except:
            continue

        offset_start = start_m - (start_hour * 60)
        duration = end_m - start_m
        if offset_start < 0: continue 

        rect_x = margin_left + (day_idx * col_width) + 1
        rect_w = col_width - 2
        top_y = height - margin_top - (offset_start * px_per_min)
        rect_h = duration * px_per_min
        rect_y = top_y - rect_h

        c.setFillAlpha(1)
        c.setFillColor(BOX_COLOR)
        c.setStrokeColor(BORDER_COLOR)
        c.roundRect(rect_x, rect_y, rect_w, rect_h, 4, fill=1, stroke=1)
        
        c.setFillColor(TEXT_COLOR)
        c.setFont("Helvetica-Bold", 9)
        course_code = exam['courseCode']
        text_w = c.stringWidth(course_code, "Helvetica-Bold", 9)
        text_x = rect_x + (rect_w - text_w) / 2
        
        if rect_h > 12:
            c.drawString(text_x, rect_y + rect_h/2 - 3, course_code)
            if rect_h > 24:
                c.setFont("Helvetica", 7)
                time_lbl = f"{start_s}-{end_s}"
                tw2 = c.stringWidth(time_lbl, "Helvetica", 7)
                c.drawString(rect_x + (rect_w - tw2)/2, rect_y + rect_h/2 - 12, time_lbl)

def main():
    if len(sys.argv) < 2:
        sys.exit(1)

    output_pdf = sys.argv[1]

    try:
        input_data = sys.stdin.read()
        if not input_data:
            sys.exit(1)
        full_data = json.loads(input_data)
    except:
        sys.exit(1)

    if isinstance(full_data, list):
        start_date_str = datetime.now().strftime("%Y-%m-%d")
        data_list = full_data
    else:
        start_date_str = full_data.get("startDate", datetime.now().strftime("%Y-%m-%d"))
        data_list = full_data.get("exams", [])

    if not data_list:
        sys.exit(1)

    global_max_day_idx = max([e['dayIndex'] for e in data_list])
    total_duration_days = max(global_max_day_idx + 1, 5)

    rooms_map = {}
    for entry in data_list:
        if not entry.get('roomNames'): continue
        r_list = [r.strip() for r in entry['roomNames'].split(',')]
        for r_name in r_list:
            if r_name not in rooms_map:
                rooms_map[r_name] = []
            rooms_map[r_name].append(entry)

    c = canvas.Canvas(output_pdf, pagesize=landscape(A4))
    w, h = landscape(A4)
    sorted_rooms = sorted(rooms_map.keys())
    
    if not sorted_rooms:
        c.setFont("Helvetica", 20)
        c.drawString(100, h/2, "No schedule data available.")
        c.showPage()
    
    for room in sorted_rooms:
        draw_grid_schedule(c, w, h, room, rooms_map[room], start_date_str, total_duration_days)
        c.showPage()

    try:
        c.save()
    except:
        sys.exit(1)

if __name__ == "__main__":
    main()