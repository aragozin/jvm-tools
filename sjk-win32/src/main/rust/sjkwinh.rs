#![allow(non_camel_case_types)]
#![allow(non_snake_case)]
#![allow(unused_parens)]

#![feature(lang_items)]
#![no_std]
#![no_main]

//use crate libc;

// no_std

#[lang = "panic_fmt"] extern fn panic_fmt() -> ! { let mut n = 0; loop { n += unsafe{ *(0 as *const c_int) };} }
#[no_mangle] pub fn rust_eh_register_frames () {}
#[no_mangle] pub fn rust_eh_unregister_frames () {}

// libc remnants
pub enum c_void {}
pub type c_char = i8;
pub type c_int = i32;

// kernel32 bindings

macro_rules! STRUCT {
    {$(#[$attrs:meta])* nodebug struct $name:ident { $($field:ident: $ftype:ty,)+ }} => {
        #[repr(C)] $(#[$attrs])*
        pub struct $name {
            $(pub $field: $ftype,)+
        }
        impl Copy for $name {}
        impl Clone for $name { fn clone(&self) -> $name { *self } }
    };
    {$(#[$attrs:meta])* struct $name:ident { $($field:ident: $ftype:ty,)+ }} => {
        #[repr(C)] #[derive(Debug)] $(#[$attrs])*
        pub struct $name {
            $(pub $field: $ftype,)+
        }
        impl Copy for $name {}
        impl Clone for $name { fn clone(&self) -> $name { *self } }
    };
}

pub type HANDLE = *mut c_void;
pub type PHANDLE = *mut HANDLE;
pub type BOOL = u32;
pub type DWORD = u32;
pub type PULONG64 = *mut u64;

STRUCT!{
    struct FILETIME {
        dwLowDateTime: DWORD,
        dwHighDateTime: DWORD,
    }
}

STRUCT!{struct THREADENTRY32 {
    dwSize: DWORD,
    cntUsage: DWORD,
    th32ThreadID: DWORD,
    th32OwnerProcessID: DWORD,
    tpBasePri: DWORD,
    tpDeltaPri: DWORD,
    dwFlags: DWORD,
    r0: u64,
    r1: u64,
    r2: u64,
}}

pub type LPFILETIME = *mut FILETIME;
pub type LPTHREADENTRY32 = *mut THREADENTRY32;

extern "system" {
    
    pub fn OpenProcess(dwDesiredAccess: DWORD, bInheritHandle: BOOL, dwProcessId: DWORD) -> HANDLE;
    pub fn OpenThread(dwDesiredAccess: DWORD, bInheritHandle: BOOL, dwThreadId: DWORD) -> HANDLE;
    pub fn CloseHandle(hObject: HANDLE) -> BOOL;
    pub fn GetLastError() -> DWORD;
    pub fn GetProcessId(Process: HANDLE) -> DWORD;
    pub fn GetProcessIdOfThread(Thread: HANDLE) -> DWORD;
    pub fn GetProcessTimes(
        hProcess: HANDLE, lpCreationTime: LPFILETIME, lpExitTime: LPFILETIME,
        lpKernelTime: LPFILETIME, lpUserTime: LPFILETIME,
    ) -> BOOL;
    pub fn GetThreadTimes(
        hThread: HANDLE, lpCreationTime: LPFILETIME, lpExitTime: LPFILETIME,
        lpKernelTime: LPFILETIME, lpUserTime: LPFILETIME,
    ) -> BOOL;
    pub fn QueryProcessCycleTime(ProcessHandle: HANDLE, CycleTime: PULONG64) -> BOOL;
    pub fn QueryThreadCycleTime(ThreadHandle: HANDLE, CycleTime: PULONG64) -> BOOL;

    pub fn CreateToolhelp32Snapshot(dwFlags: DWORD, th32ProcessID: DWORD) -> HANDLE;
    pub fn Thread32First(hSnapshot: HANDLE, lpte: LPTHREADENTRY32) -> BOOL;
    pub fn Thread32Next(hSnapshot: HANDLE, lpte: LPTHREADENTRY32) -> BOOL;

}

// JNI declarions

pub type jint = i32;
pub type jlong = i64;
pub type jbyte = i8;
pub type jboolean = u8;
pub type jchar = u16;
pub type jshort = i16;
pub type jfloat = f32;
pub type jdouble = f64;
pub type jsize = jint;

pub enum _jobject {}
pub type jobject = *mut _jobject;

pub enum _jclass {}
pub type jclass = *mut _jclass;

pub type jthrowable = jobject;
pub type jstring = jobject;
pub type jarray = jobject;
pub type jbooleanArray = jarray;
pub type jbyteArray = jarray;
pub type jcharArray = jarray;
pub type jshortArray = jarray;
pub type jintArray = jarray;
pub type jlongArray = jarray;
pub type jfloatArray = jarray;
pub type jdoubleArray = jarray;
pub type jobjectArray = jarray;
pub type jweak = jobject;

pub enum _jmethodID {}
pub type jmethodID = *mut _jmethodID;

pub struct jvalue(jobject);

pub type UnmappedFunction = *const c_void;

#[repr(C)]
pub struct JNINativeInterface {
    reserved0: UnmappedFunction,
    reserved1: UnmappedFunction,
    reserved2: UnmappedFunction,
    reserved3: UnmappedFunction,
    GetVersion: unsafe extern "C" fn(*mut JNIEnv) -> jint,
    DefineClass: UnmappedFunction,
    FindClass: UnmappedFunction,
    FromReflectedMethod: UnmappedFunction,
    FromReflectedField: UnmappedFunction,
    ToReflectedMethod: UnmappedFunction,
    GetSuperclass: UnmappedFunction,
    IsAssignableFrom: UnmappedFunction,
    ToReflectedField: UnmappedFunction,
    Throw: UnmappedFunction,
    ThrowNew: UnmappedFunction,
    ExceptionOccurred: UnmappedFunction,
    ExceptionDescribe: UnmappedFunction,
    ExceptionClear: UnmappedFunction,
    FatalError: UnmappedFunction,
    PushLocalFrame: UnmappedFunction,
    PopLocalFrame: UnmappedFunction,
    NewGlobalRef: UnmappedFunction,
    DeleteGlobalRef: UnmappedFunction,
    DeleteLocalRef: UnmappedFunction,
    IsSameObject: UnmappedFunction,
    NewLocalRef: UnmappedFunction,
    EnsureLocalCapacity: UnmappedFunction,
    AllocObject: UnmappedFunction,
    NewObject: UnmappedFunction,
    NewObjectV: UnmappedFunction,
    NewObjectA: UnmappedFunction,
    GetObjectClass: unsafe extern "C" fn (*mut JNIEnv, obj: jobject) -> jclass,
    IsInstanceOf: UnmappedFunction,
    GetMethodId: unsafe extern "C" fn (*mut JNIEnv, class: jclass, name: *const c_char, sig: *const c_char) -> jmethodID,
    CallObjectMethod: UnmappedFunction,
    CallObjectMethodV: UnmappedFunction,
    CallObjectMethodA: unsafe extern "C" fn (*mut JNIEnv, obj: jobject, methodID: jmethodID, args: *const jvalue) -> jobject,
    CallBooleanMethod: UnmappedFunction,
    CallBooleanMethodV: UnmappedFunction,
    CallBooleanMethodA: UnmappedFunction,
    CallByteMethod: UnmappedFunction,
    CallByteMethodV: UnmappedFunction,
    CallByteMethodA: UnmappedFunction,
    CallCharMethod: UnmappedFunction,
    CallCharMethodV: UnmappedFunction,
    CallCharMethodA: UnmappedFunction,
    CallShortMethod: UnmappedFunction,
    CallShortMethodV: UnmappedFunction,
    CallShortMethodA: UnmappedFunction,
    CallIntMethod: UnmappedFunction,
    CallIntMethodV: UnmappedFunction,
    CallIntMethodA: UnmappedFunction,
    CallLongMethod: UnmappedFunction,
    CallLongMethodV: UnmappedFunction,
    CallLongMethodA: UnmappedFunction,
    CallFloatMethod: UnmappedFunction,
    CallFloatMethodV: UnmappedFunction,
    CallFloatMethodA: UnmappedFunction,
    CallDoubleMethod: UnmappedFunction,
    CallDoubleMethodV: UnmappedFunction,
    CallDoubleMethodA: UnmappedFunction,
    CallVoidMethod: UnmappedFunction,
    CallVoidMethodV: UnmappedFunction,
    CallVoidMethodA: UnmappedFunction,
    CallNonvirtualObjectMethod: UnmappedFunction,
    CallNonvirtualObjectMethodV: UnmappedFunction,
    CallNonvirtualObjectMethodA: UnmappedFunction,
    CallNonvirtualBooleanMethod: UnmappedFunction,
    CallNonvirtualBooleanMethodV: UnmappedFunction,
    CallNonvirtualBooleanMethodA: UnmappedFunction,
    CallNonvirtualByteMethod: UnmappedFunction,
    CallNonvirtualByteMethodV: UnmappedFunction,
    CallNonvirtualByteMethodA: UnmappedFunction,
    CallNonvirtualCharMethod: UnmappedFunction,
    CallNonvirtualCharMethodV: UnmappedFunction,
    CallNonvirtualCharMethodA: UnmappedFunction,
    CallNonvirtualShortMethod: UnmappedFunction,
    CallNonvirtualShortMethodV: UnmappedFunction,
    CallNonvirtualShortMethodA: UnmappedFunction,
    CallNonvirtualIntMethod: UnmappedFunction,
    CallNonvirtualIntMethodV: UnmappedFunction,
    CallNonvirtualIntMethodA: UnmappedFunction,
    CallNonvirtualLongMethod: UnmappedFunction,
    CallNonvirtualLongMethodV: UnmappedFunction,
    CallNonvirtualLongMethodA: UnmappedFunction,
    CallNonvirtualFloatMethod: UnmappedFunction,
    CallNonvirtualFloatMethodV: UnmappedFunction,
    CallNonvirtualFloatMethodA: UnmappedFunction,
    CallNonvirtualDoubleMethod: UnmappedFunction,
    CallNonvirtualDoubleMethodV: UnmappedFunction,
    CallNonvirtualDoubleMethodA: UnmappedFunction,
    CallNonvirtualVoidMethod: UnmappedFunction,
    CallNonvirtualVoidMethodV: UnmappedFunction,
    CallNonvirtualVoidMethodA: UnmappedFunction,
    GetFieldID: UnmappedFunction,
    GetObjectField: UnmappedFunction,
    GetBooleanField: UnmappedFunction,
    GetByteField: UnmappedFunction,
    GetCharField: UnmappedFunction,
    GetShortField: UnmappedFunction,
    GetIntField: UnmappedFunction,
    GetLongField: UnmappedFunction,
    GetFloatField: UnmappedFunction,
    GetDoubleField: UnmappedFunction,
    SetObjectField: UnmappedFunction,
    SetBooleanField: UnmappedFunction,
    SetByteField: UnmappedFunction,
    SetCharField: UnmappedFunction,
    SetShortField: UnmappedFunction,
    SetIntField: UnmappedFunction,
    SetLongField: UnmappedFunction,
    SetFloatField: UnmappedFunction,
    SetDoubleField: UnmappedFunction,
    GetStaticMethodID: UnmappedFunction,
    CallStaticObjectMethod: UnmappedFunction,
    CallStaticObjectMethodV: UnmappedFunction,
    CallStaticObjectMethodA: UnmappedFunction,
    CallStaticBooleanMethod: UnmappedFunction,
    CallStaticBooleanMethodV: UnmappedFunction,
    CallStaticBooleanMethodA: UnmappedFunction,
    CallStaticByteMethod: UnmappedFunction,
    CallStaticByteMethodV: UnmappedFunction,
    CallStaticByteMethodA: UnmappedFunction,
    CallStaticCharMethod: UnmappedFunction,
    CallStaticCharMethodV: UnmappedFunction,
    CallStaticCharMethodA: UnmappedFunction,
    CallStaticShortMethod: UnmappedFunction,
    CallStaticShortMethodV: UnmappedFunction,
    CallStaticShortMethodA: UnmappedFunction,
    CallStaticIntMethod: UnmappedFunction,
    CallStaticIntMethodV: UnmappedFunction,
    CallStaticIntMethodA: UnmappedFunction,
    CallStaticLongMethod: UnmappedFunction,
    CallStaticLongMethodV: UnmappedFunction,
    CallStaticLongMethodA: UnmappedFunction,
    CallStaticFloatMethod: UnmappedFunction,
    CallStaticFloatMethodV: UnmappedFunction,
    CallStaticFloatMethodA: UnmappedFunction,
    CallStaticDoubleMethod: UnmappedFunction,
    CallStaticDoubleMethodV: UnmappedFunction,
    CallStaticDoubleMethodA: UnmappedFunction,
    CallStaticVoidMethod: UnmappedFunction,
    CallStaticVoidMethodV: UnmappedFunction,
    CallStaticVoidMethodA: UnmappedFunction,
    GetStaticFieldID: UnmappedFunction,
    GetStaticObjectField: UnmappedFunction,
    GetStaticBooleanField: UnmappedFunction,
    GetStaticByteField: UnmappedFunction,
    GetStaticCharField: UnmappedFunction,
    GetStaticShortField: UnmappedFunction,
    GetStaticIntField: UnmappedFunction,
    GetStaticLongField: UnmappedFunction,
    GetStaticFloatField: UnmappedFunction,
    GetStaticDoubleField: UnmappedFunction,
    SetStaticObjectField: UnmappedFunction,
    SetStaticBooleanField: UnmappedFunction,
    SetStaticByteField: UnmappedFunction,
    SetStaticCharField: UnmappedFunction,
    SetStaticShortField: UnmappedFunction,
    SetStaticIntField: UnmappedFunction,
    SetStaticLongField: UnmappedFunction,
    SetStaticFloatField: UnmappedFunction,
    SetStaticDoubleField: UnmappedFunction,
    NewString: UnmappedFunction,
    GetStringLength: UnmappedFunction,
    GetStringChars: UnmappedFunction,
    ReleaseStringChars: UnmappedFunction,
    NewStringUTF: UnmappedFunction,
    GetStringUTFLength: UnmappedFunction,
    GetStringUTFChars: UnmappedFunction,
    ReleaseStringUTFChars: UnmappedFunction,
    GetArrayLength: unsafe fn(env: *mut JNIEnv, array: jarray) -> jsize,
    NewObjectArray: UnmappedFunction,
    GetObjectArrayElement: UnmappedFunction,
    SetObjectArrayElement: UnmappedFunction,
    NewBooleanArray: UnmappedFunction,
    NewByteArray: UnmappedFunction,
    NewCharArray: UnmappedFunction,
    NewShortArray: UnmappedFunction,
    NewIntArray: UnmappedFunction,
    NewLongArray: UnmappedFunction,
    NewFloatArray: UnmappedFunction,
    NewDoubleArray: UnmappedFunction,
    GetBooleanArrayElements: UnmappedFunction,
    GetByteArrayElements: UnmappedFunction,
    GetCharArrayElements: UnmappedFunction,
    GetShortArrayElements: UnmappedFunction,
    GetIntArrayElements: unsafe extern "C" fn(env: *mut JNIEnv, array: jintArray, isCopy: *mut jboolean) -> *mut jint,
    GetLongArrayElements: UnmappedFunction,
    GetFloatArrayElements: UnmappedFunction,
    GetDoubleArrayElements: UnmappedFunction,
    ReleaseBooleanArrayElements: UnmappedFunction,
    ReleaseByteArrayElements: UnmappedFunction,
    ReleaseCharArrayElements: UnmappedFunction,
    ReleaseShortArrayElements: UnmappedFunction,
    ReleaseIntArrayElements: unsafe extern "C" fn(env: *mut JNIEnv, array: jintArray, elems: *mut jint, mode: jint),
    ReleaseLongArrayElements: UnmappedFunction,
    ReleaseFloatArrayElements: UnmappedFunction,
    ReleaseDoubleArrayElements: UnmappedFunction,
    GetBooleanArrayRegion: UnmappedFunction,
    GetByteArrayRegion: UnmappedFunction,
    GetCharArrayRegion: UnmappedFunction,
    GetShortArrayRegion: UnmappedFunction,
    GetIntArrayRegion: UnmappedFunction,
    GetLongArrayRegion: UnmappedFunction,
    GetFloatArrayRegion: UnmappedFunction,
    GetDoubleArrayRegion: UnmappedFunction,
    SetBooleanArrayRegion: UnmappedFunction,
    SetByteArrayRegion: UnmappedFunction,
    SetCharArrayRegion: UnmappedFunction,
    SetShortArrayRegion: UnmappedFunction,
    SetIntArrayRegion: extern "C" fn(env: *mut JNIEnv, array: jintArray, start: jsize, len: jsize, buf: *const jint),
    SetLongArrayRegion: UnmappedFunction,
    SetFloatArrayRegion: UnmappedFunction,
    SetDoubleArrayRegion: UnmappedFunction,
    RegisterNatives: UnmappedFunction,
    UnregisterNatives: UnmappedFunction,
    MonitorEnter: UnmappedFunction,
    MonitorExit: UnmappedFunction,
    GetJavaVM: UnmappedFunction,
    GetStringRegion: UnmappedFunction,
    GetStringUTFRegion: UnmappedFunction,
    GetPrimitiveArrayCritical: UnmappedFunction,
    ReleasePrimitiveArrayCritical: UnmappedFunction,
    GetStringCritical: UnmappedFunction,
    ReleaseStringCritical: UnmappedFunction,
    NewWeakGlobalRef: UnmappedFunction,
    DeleteWeakGlobalRef: UnmappedFunction,
    ExceptionCheck: extern "C" fn(env: *mut JNIEnv) -> jboolean,
    NewDirectByteBuffer: extern "C" fn(env: *mut JNIEnv, address: *mut c_void, capacity: jlong) -> jobject,
    GetDirectBufferAddress: extern "C" fn(env: *mut JNIEnv, buf: jobject) -> *mut c_void,
    GetDirectBufferCapacity: extern "C" fn(env: *mut JNIEnv, buf: jobject) -> jlong,
    GetObjectRefType: UnmappedFunction,
}

#[repr(C)]
pub struct JNIEnv {
    functions: *mut JNINativeInterface
}

impl JNIEnv {

    fn _this(&mut self) -> *mut Self {
        self as *mut Self
    }

//    fn GetMethodID(&mut self, class: jclass, name: &str, sig: &str) -> jmethodID {
//        let name = CString::new(name).unwrap();
//        let sig = CString::new(sig).unwrap();
//
//
//        unsafe { ((*self.functions).GetMethodId)(self._this(), class, name.as_ptr(), sig.as_ptr()) }
//    }

    fn GetObjectClass(&mut self, obj: jobject) -> jclass {
        unsafe { ((*self.functions).GetObjectClass)(self._this(), obj) }
    }

    fn GetVersion(&mut self) -> jint {
        unsafe { ((*self.functions).GetVersion)(self._this()) }
    }

    fn CallObjectMethod(&mut self, obj: jobject, methodID: jmethodID, args: &[jvalue]) -> jobject {
        unsafe { 
            let pargs = args.as_ptr();
            ((*self.functions).CallObjectMethodA)(self._this(), obj, methodID, pargs)
        }
    }

    fn SetIntArrayRegion(&mut self, array: jintArray, start: usize, len: usize, buf: &[jint]) {
        unsafe {
            let pbuf = buf.as_ptr();
            ((*self.functions).SetIntArrayRegion)(self._this(), array, start as jsize, len as jsize, pbuf)
        }
    }

    fn GetArrayLength(&mut self, array: jarray) -> jsize {
        unsafe {
            ((*self.functions).GetArrayLength)(self._this(), array)
        }
    }
}

// Native methods implementations

#[no_mangle]
pub extern fn Java_org_gridkit_jvmtool_win32_SjkWinHelper_GetProcessTimes(env: *mut JNIEnv, _: jobject, pid: jint, buffer: jintArray) -> jint {

    let mut ctime = FILETIME { dwLowDateTime: 0, dwHighDateTime: 0 };
    let mut etime = FILETIME { dwLowDateTime: 0, dwHighDateTime: 0 };
    let mut ktime = FILETIME { dwLowDateTime: 0, dwHighDateTime: 0 };
    let mut utime = FILETIME { dwLowDateTime: 0, dwHighDateTime: 0 };

    let pctime = &mut ctime;
    let petime = &mut etime;
    let pktime = &mut ktime;
    let putime = &mut utime;

    let ph = unsafe{ OpenProcess(0x0400, 0, pid as u32) }; // PROCESS_QUERY_INFORMATION 

    if (ph == (0 as HANDLE)) {
        return unsafe{ GetLastError() } as jint;
    }

//    println!("GetProcessId {} -> {}", (ph as u64), unsafe{ GetProcessId(ph) });

    if (unsafe{ GetProcessTimes(ph, pctime, petime, pktime, putime) } == 0) {
        unsafe{ CloseHandle(ph) };
        return unsafe{ GetLastError() } as jint;
    }

    unsafe{ CloseHandle(ph) };

//    println!("GetProcessTimes {} SUCCESS", pid);
//    println!("utime {} {}", putime[0].dwLowDateTime, putime[0].dwHighDateTime);

    let data = [
        pctime.dwLowDateTime as jint, pctime.dwHighDateTime as jint,
        petime.dwLowDateTime as jint, petime.dwHighDateTime as jint,
        pktime.dwLowDateTime as jint, pktime.dwHighDateTime as jint,
        putime.dwLowDateTime as jint, putime.dwHighDateTime as jint];

    unsafe {
        (*env).SetIntArrayRegion(buffer, 0, data.len(), &data);
    }

    return 0;
}

#[no_mangle]
pub extern fn Java_org_gridkit_jvmtool_win32_SjkWinHelper_GetThreadTimes(env: *mut JNIEnv, _: jobject, pid: jint, buffer: jintArray) -> jint {

    let mut ctime = FILETIME { dwLowDateTime: 0, dwHighDateTime: 0 };
    let mut etime = FILETIME { dwLowDateTime: 0, dwHighDateTime: 0 };
    let mut ktime = FILETIME { dwLowDateTime: 0, dwHighDateTime: 0 };
    let mut utime = FILETIME { dwLowDateTime: 0, dwHighDateTime: 0 };

    let pctime = &mut ctime;
    let petime = &mut etime;
    let pktime = &mut ktime;
    let putime = &mut utime;

    let ph = unsafe{ OpenThread(0x0040, 0, pid as u32) }; // THREAD_QUERY_INFORMATION

    if (ph == (0 as HANDLE)) {
        return unsafe{ GetLastError() } as jint;
    }

//    println!("GetProcessId {} -> {}", (ph as u64), unsafe{ GetProcessId(ph) });

    if (unsafe{ GetThreadTimes(ph, pctime, petime, pktime, putime) } == 0) {
        unsafe{ CloseHandle(ph) };
        return unsafe{ GetLastError() } as jint;
    }

    unsafe{ CloseHandle(ph) };

//    println!("GetProcessTimes {} SUCCESS", pid);
//    println!("utime {} {}", putime[0].dwLowDateTime, putime[0].dwHighDateTime);

    let data = [
        pctime.dwLowDateTime as jint, pctime.dwHighDateTime as jint,
        petime.dwLowDateTime as jint, petime.dwHighDateTime as jint,
        pktime.dwLowDateTime as jint, pktime.dwHighDateTime as jint,
        putime.dwLowDateTime as jint, putime.dwHighDateTime as jint];

    unsafe {
        (*env).SetIntArrayRegion(buffer, 0, data.len(), &data);
    }

    return 0;
}

#[no_mangle]
pub extern fn Java_org_gridkit_jvmtool_win32_SjkWinHelper_QueryProcessCycleTime(env: *mut JNIEnv, _: jobject, pid: jint) -> jlong {

    let mut cc = 0 as u64;

    let pcc = &mut cc;

    let ph = unsafe{ OpenProcess(0x0400, 0, pid as u32) }; // PROCESS_QUERY_INFORMATION 

    if (ph == (0 as HANDLE)) {
        return -(unsafe{ GetLastError() } as jlong);
    }

    if (unsafe{ QueryProcessCycleTime(ph, pcc) } == 0) {
        unsafe{ CloseHandle(ph) };
        return -(unsafe{ GetLastError() } as jlong);
    }

    unsafe{ CloseHandle(ph) };

    return (*pcc) as jlong;
}

#[no_mangle]
pub extern fn Java_org_gridkit_jvmtool_win32_SjkWinHelper_QueryThreadCycleTime(env: *mut JNIEnv, _: jobject, pid: jint) -> jlong {

    let mut cc = 0 as u64;

    let pcc = &mut cc;

    let ph = unsafe{ OpenThread(0x0040, 0, pid as u32) }; // THREAD_QUERY_INFORMATION 

    if (ph == (0 as HANDLE)) {
        return -(unsafe{ GetLastError() } as jlong);
    }

    if (unsafe{ QueryThreadCycleTime(ph, pcc) } == 0) {
        unsafe{ CloseHandle(ph) };
        return -(unsafe{ GetLastError() } as jlong);
    }

    unsafe{ CloseHandle(ph) };

    return (*pcc) as jlong;
}

#[no_mangle]
pub extern fn Java_org_gridkit_jvmtool_win32_SjkWinHelper_EnumThreads(env: *mut JNIEnv, _: jobject, pid: jint, buffer: jintArray) -> jint {

    let bufsize = unsafe { (*env).GetArrayLength(buffer) } as usize;

    if (bufsize > 1024) {
        return -1000000; // magic error    
    }

    // calling JNI is corrpting stack frame, probably due to ABI missmatch
    // preallocate 1024 slots on stack, to make single call before return    
    let mut sbuf = [ 0 as jint; 1024];

    let ph = unsafe { CreateToolhelp32Snapshot(0x04, 0) }; // TH32CS_SNAPTHREAD

    if (ph == (0 as HANDLE)) {
        return -(unsafe{ GetLastError() } as jint);
    }

    let mut te = THREADENTRY32 { dwSize: 28, cntUsage: 0, th32ThreadID: 0, th32OwnerProcessID: 0, tpBasePri: 0, tpDeltaPri: 0, dwFlags: 0, r0: 0, r1: 0, r2: 0 };
    let mut pte = &mut te;

    let mut n: usize = 0;

    let mut rs = unsafe { Thread32First(ph, pte) };

    while(rs != 0) {

        if (pte.th32OwnerProcessID as jint == pid) {
           if (n < bufsize) {
               sbuf[n] = pte.th32ThreadID as jint;
               let data = [ pte.th32ThreadID as jint ];
           }
           n = n + 1;
        }      

        rs = unsafe { Thread32Next(ph, pte) };
        //println!("Thread32Next {}", rs);
    }

    let cr = unsafe{ GetLastError() };

    unsafe{ CloseHandle(ph) };

    if (cr != 18) { // ERROR_NO_MORE_FILES
        return -(cr as jint);
    }

    let m = if n < bufsize { n } else { bufsize };
    unsafe {
        (*env).SetIntArrayRegion(buffer, 0, m, &sbuf);
    }
	
    return n as jint;
}

/*
fn dump(h: HANDLE, size: usize) {

    let mut ti = THREADENTRY32 { dwSize: 0, cntUsage: 0, th32ThreadID: 0, th32OwnerProcessID: 0, tpBasePri: 0, tpDeltaPri: 0, dwFlags: 0, r0: 0, r1: 0, r2: 0 };

    ti.dwSize = size as DWORD;

    let mut rs = unsafe { Thread32First(h, &mut ti) };
    let ec = unsafe{ GetLastError() };

    println!("Thread32First {} {} {}", h as u32, rs, ec);
    println!("ti.dwSize             {}", ti.dwSize);
    println!("ti.cntUsage           {}", ti.cntUsage);
    println!("ti.th32ThreadID       {}", ti.th32ThreadID);
    println!("ti.th32OwnerProcessID {}", ti.th32OwnerProcessID);
    println!("ti.tpBasePri          {}", ti.tpBasePri);
    println!("ti.tpDeltaPri         {}", ti.tpDeltaPri);
    println!("ti.dwFlags            {}", ti.dwFlags);
    println!("ti.r0                 {}", ti.r0);
    println!("ti.r1                 {}", ti.r1);
}
*/