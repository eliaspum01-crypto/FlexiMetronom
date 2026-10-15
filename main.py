import json, os, threading, time, tkinter as tk
from tkinter import ttk, messagebox, simpledialog

PRESETS_FILE = os.path.join(os.path.expanduser("~"), ".fleximetronom_presets.json")

class FlexiMetronom:
    def __init__(self, root):
        self.root=root; root.title("FlexiMetronom – Musiker-Edition"); root.geometry("850x700"); root.minsize(740,620); root.configure(bg="#101318")
        self.running=False; self.stop_event=threading.Event(); self.tick_index=0; self.presets=self.load_presets()
        self.bpm=tk.IntVar(value=120); self.numerator=tk.IntVar(value=10); self.denominator=tk.IntVar(value=16)
        self.beat_unit=tk.StringVar(value="1/16"); self.sound=tk.StringVar(value="Click"); self.grouping=tk.StringVar(value="3+3+4"); self.accents=tk.StringVar(value="1"); self.status=tk.StringVar(value="Bereit")
        self.build_ui(); root.protocol("WM_DELETE_WINDOW", self.close)
    def build_ui(self):
        style=ttk.Style()
        try: style.theme_use("clam")
        except tk.TclError: pass
        style.configure("TCombobox",fieldbackground="#202630",background="#202630",foreground="#f5f7fa")
        style.configure("TScale",background="#191e26",troughcolor="#303743")
        h=tk.Frame(self.root,bg="#101318"); h.pack(fill="x",padx=26,pady=(20,8))
        tk.Label(h,text="FLEXI",font=("Segoe UI",12,"bold"),fg="#8bd7b2",bg="#101318").pack(anchor="w")
        tk.Label(h,text="METRONOM",font=("Segoe UI",28,"bold"),fg="#f5f7fa",bg="#101318").pack(anchor="w")
        tk.Label(h,text="MUSIKER-EDITION  •  UNGERADE TAKTE  •  FLEXIBLE AKZENTE",font=("Segoe UI",9),fg="#a5adba",bg="#101318").pack(anchor="w")
        top=tk.Frame(self.root,bg="#191e26",highlightbackground="#303743",highlightthickness=1); top.pack(fill="x",padx=26,pady=12)
        l=tk.Frame(top,bg="#191e26"); l.pack(side="left",fill="both",expand=True,padx=20,pady=18)
        tk.Label(l,text="TEMPO",bg="#191e26",fg="#9ba6b5",font=("Segoe UI",10,"bold")).pack(anchor="w")
        self.bpm_label=tk.Label(l,text="120 BPM",bg="#191e26",fg="white",font=("Segoe UI",30,"bold")); self.bpm_label.pack(anchor="w",pady=(3,8))
        sc=ttk.Scale(l,from_=20,to=300,orient="horizontal",command=lambda v:self.set_bpm(round(float(v)))); sc.set(120); sc.pack(fill="x")
        row=tk.Frame(l,bg="#191e26"); row.pack(fill="x",pady=(12,0))
        tk.Button(row,text="−",command=lambda:self.set_bpm(self.bpm.get()-1),bg="#303743",fg="white",relief="flat",width=4).pack(side="left")
        tk.Button(row,text="+",command=lambda:self.set_bpm(self.bpm.get()+1),bg="#303743",fg="white",relief="flat",width=4).pack(side="left",padx=7)
        self.play=tk.Button(row,text="▶ START",command=self.toggle,bg="#8bd7b2",fg="#101318",relief="flat",font=("Segoe UI",11,"bold"),padx=22,pady=8); self.play.pack(side="right")
        r=tk.Frame(top,bg="#191e26"); r.pack(side="right",padx=24,pady=18)
        tk.Label(r,text="TAKTART",bg="#191e26",fg="#9ba6b5",font=("Segoe UI",10,"bold")).grid(row=0,column=0,columnspan=3,sticky="w",pady=(0,8))
        tk.Spinbox(r,from_=1,to=64,textvariable=self.numerator,width=5,bg="#202630",fg="white",buttonbackground="#303743",relief="flat",font=("Segoe UI",15)).grid(row=1,column=0)
        tk.Label(r,text="/",bg="#191e26",fg="white",font=("Segoe UI",20,"bold")).grid(row=1,column=1,padx=6)
        ttk.Combobox(r,textvariable=self.denominator,values=(1,2,4,8,16,32),width=5,state="readonly").grid(row=1,column=2)
        p=tk.Frame(self.root,bg="#191e26",highlightbackground="#303743",highlightthickness=1); p.pack(fill="both",expand=True,padx=26,pady=(0,12))
        tk.Label(p,text="RHYTHMUS & AKZENTE",bg="#191e26",fg="#f5f7fa",font=("Segoe UI",13,"bold")).pack(anchor="w",padx=20,pady=(17,12))
        g=tk.Frame(p,bg="#191e26"); g.pack(fill="x",padx=20)
        def label(text,row,col): tk.Label(g,text=text,bg="#191e26",fg="#b7c0cc").grid(row=row,column=col,sticky="w",pady=7)
        label("Zähleinheit",0,0); ttk.Combobox(g,textvariable=self.beat_unit,values=("1/4","1/8","1/16","1/32"),width=12,state="readonly").grid(row=0,column=1,sticky="w",padx=14)
        label("Klang",0,2); ttk.Combobox(g,textvariable=self.sound,values=("Click","Woodblock","Bell"),width=14,state="readonly").grid(row=0,column=3,sticky="w",padx=14)
        label("Gruppierung",1,0); tk.Entry(g,textvariable=self.grouping,bg="#202630",fg="white",insertbackground="white",relief="flat",width=20).grid(row=1,column=1,sticky="w",padx=14,ipady=6)
        tk.Label(g,text="z. B. 3+3+4",bg="#191e26",fg="#778392").grid(row=1,column=2,columnspan=2,sticky="w",padx=(24,0))
        label("Betonte Zählzeiten",2,0); tk.Entry(g,textvariable=self.accents,bg="#202630",fg="white",insertbackground="white",relief="flat",width=20).grid(row=2,column=1,sticky="w",padx=14,ipady=6)
        tk.Label(g,text="z. B. 1,4,7 (1-basiert)",bg="#191e26",fg="#778392").grid(row=2,column=2,columnspan=2,sticky="w",padx=(24,0))
        tk.Label(p,text="TAKT-VORSCHAU",bg="#191e26",fg="#9ba6b5",font=("Segoe UI",10,"bold")).pack(anchor="w",padx=20,pady=(22,8))
        self.beats=tk.Frame(p,bg="#191e26"); self.beats.pack(fill="x",padx=20)
        tk.Label(p,text="Akzentpositionen werden mintfarben hervorgehoben.",bg="#191e26",fg="#778392",font=("Segoe UI",9)).pack(anchor="w",padx=20,pady=(8,16))
        bar=tk.Frame(self.root,bg="#101318"); bar.pack(fill="x",padx=26,pady=(0,18))
        tk.Label(bar,text="PRESETS",bg="#101318",fg="#9ba6b5",font=("Segoe UI",9,"bold")).pack(side="left")
        self.combo=ttk.Combobox(bar,values=list(self.presets),width=22,state="readonly"); self.combo.pack(side="left",padx=10)
        tk.Button(bar,text="Laden",command=self.load_selected,bg="#303743",fg="white",relief="flat").pack(side="left")
        tk.Button(bar,text="Speichern unter…",command=self.save_preset,bg="#303743",fg="white",relief="flat").pack(side="left",padx=7)
        tk.Label(bar,textvariable=self.status,bg="#101318",fg="#8bd7b2").pack(side="right")
        for v in (self.numerator,self.accents): v.trace_add("write",lambda *_:self.render_beats())
        self.render_beats()
    def set_bpm(self,v):
        v=max(20,min(300,int(v))); self.bpm.set(v); self.bpm_label.config(text=f"{v} BPM")
    def render_beats(self):
        if not hasattr(self,"beats"): return
        for w in self.beats.winfo_children(): w.destroy()
        try: n=max(1,min(64,int(self.numerator.get())))
        except Exception: n=10
        try: a={int(x.strip()) for x in self.accents.get().split(",") if x.strip()}
        except Exception: a={1}
        for i in range(min(n,32)):
            on=i+1 in a
            tk.Label(self.beats,text=str(i+1),bg="#8bd7b2" if on else "#303743",fg="#101318" if on else "#e7ebf0",font=("Segoe UI",9,"bold"),width=3,pady=8).grid(row=i//16,column=i%16,padx=3,pady=3,sticky="ew")
        for c in range(16): self.beats.grid_columnconfigure(c,weight=1)
    def period(self):
        den=int(self.beat_unit.get().split("/")[1]); return max(.025,60/max(20,self.bpm.get())*(4/den))
    def tick(self,accent):
        try:
            import winsound
            s=self.sound.get()
            freq,dur=({"Bell":(1500 if accent else 1100,100 if accent else 65),"Woodblock":(1050 if accent else 750,55 if accent else 35)}.get(s,(1800 if accent else 1000,45 if accent else 28)))
            winsound.Beep(freq,dur)
        except Exception: pass
    def toggle(self): self.stop() if self.running else self.start()
    def start(self):
        self.running=True; self.stop_event.clear(); self.tick_index=0; self.play.config(text="■ STOP",bg="#f08a8a"); self.status.set("Läuft")
        threading.Thread(target=self.loop,daemon=True).start()
    def stop(self):
        self.running=False; self.stop_event.set()
        if hasattr(self,"play"): self.play.config(text="▶ START",bg="#8bd7b2")
        self.status.set("Gestoppt")
    def loop(self):
        nxt=time.perf_counter()
        while not self.stop_event.is_set():
            try: n=max(1,min(64,int(self.numerator.get())))
            except Exception: n=10
            try: accents={int(x.strip()) for x in self.accents.get().split(",") if x.strip()}
            except Exception: accents={1}
            pos=self.tick_index%n+1
            self.root.after(0,lambda p=pos:self.status.set(f"Zählzeit {p}/{self.numerator.get()}"))
            self.tick(pos in accents); self.tick_index+=1; nxt+=self.period()
            wait=nxt-time.perf_counter()
            if wait>0:self.stop_event.wait(wait)
            else:nxt=time.perf_counter()
    def current(self):
        return dict(bpm=self.bpm.get(),numerator=self.numerator.get(),denominator=self.denominator.get(),beat_unit=self.beat_unit.get(),sound=self.sound.get(),grouping=self.grouping.get(),accents=self.accents.get())
    def load_presets(self):
        try:
            with open(PRESETS_FILE,encoding="utf-8") as f:return json.load(f)
        except Exception:return {}
    def save_preset(self):
        name=simpledialog.askstring("Preset speichern","Name für dieses Preset:",parent=self.root)
        if not name:return
        self.presets[name]=self.current()
        try:
            with open(PRESETS_FILE,"w",encoding="utf-8") as f:json.dump(self.presets,f,ensure_ascii=False,indent=2)
            self.combo.configure(values=list(self.presets)); self.combo.set(name); self.status.set(f"Preset „{name}“ gespeichert")
        except Exception as e:messagebox.showerror("Fehler",str(e))
    def load_selected(self):
        name=self.combo.get()
        if name not in self.presets:messagebox.showinfo("Presets","Bitte zuerst ein Preset auswählen.");return
        s=self.presets[name]; self.set_bpm(s.get("bpm",120)); self.numerator.set(s.get("numerator",10)); self.denominator.set(s.get("denominator",16)); self.beat_unit.set(s.get("beat_unit","1/16")); self.sound.set(s.get("sound","Click")); self.grouping.set(s.get("grouping","3+3+4")); self.accents.set(s.get("accents","1")); self.render_beats()
    def close(self):self.stop(); self.root.destroy()

if __name__=="__main__":
    root=tk.Tk(); app=FlexiMetronom(root); root.mainloop()
