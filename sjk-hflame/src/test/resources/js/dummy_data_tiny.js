var data = {
    frames: [
        "(WAITING)",
        "(BLOCKED)",
        "(RUNNABLE)",
        "(TIMED_WAITING)",
        "(IO)",
        "Thread.run", // 5
        "Object.wait",
        "MyClass.a",
        "MyClass.b",
        "MyClass.c",
        "MyMain.run"
    ],
    frameColors: [
        null,
        null,
        null,
        null,
        null,
        "#f65", // 5
        "#f56",
        "#f74",
        "#f47",
        "#e66",
        "#e57"                    
    ],
    threads: [
        {
            name: "Main",
            traces: [
                { trace: [5, 6, 0], samples: 100 },
                { trace: [5, 7, 1], samples: 10 },
                { trace: [5, 7, 2], samples: 10 },
                { trace: [5, 7, 3], samples: 10 },
                { trace: [5, 7, 8, 2], samples: 15},
                { trace: [5, 7, 9, 2], samples: 15}
            ]
        },
        {
            name: "MyThread1",
            traces: [
                { trace: [10, 7, 9, 2], samples: 25}
            ]
        },
        {
            name: "MyThread2",
            traces: [
                { trace: [10, 7, 9, 8, 2], samples: 25}
            ]
        }
    ]
}            
