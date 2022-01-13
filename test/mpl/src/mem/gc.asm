EXTERN malloc
EXTERN free
EXTERN __data_start
EXTERN _end
EXTERN printf
GLOBAL gc_lowerStackAddress
GLOBAL gc_dataEndAddress
GLOBAL gc_heapMemoryStart
GLOBAL gc_heapMemoryEnd
GLOBAL gc_heapMemory
GLOBAL gc_IsHeapAddress_v
GLOBAL gc_GCInit_v

SECTION .text
gc_IsHeapAddress_v:
	; Create new stack frame
	push ebp
	mov ebp, esp
	; Save important registers
	push ebx
	push ecx

	; if(value >= heapMemoryStart) {
	; Condition testing (value >= heapMemoryStart)
	mov eax, DWORD [ebp + 8]
	mov ebx, DWORD [gc_heapMemoryStart + 0]
	cmp eax, ebx
	jl .if4_end
	.if4:
		; if(value < heapMemoryEnd) {
		; Condition testing (value < heapMemoryEnd)
		mov eax, DWORD [ebp + 8]
		mov ebx, DWORD [gc_heapMemoryEnd + 0]
		cmp eax, ebx
		jge .if5_end
		.if5:
			; return 1
			mov eax, 1
			jmp .return
		
		; }
		.if5_end:
	
	; }
	.if4_end:

	; return 0
	mov eax, 0
	jmp .return

.return:
	; Free up stack space
	add esp, 0
	; Reset registers
	pop ecx
	pop ebx
	; Reset stack frame and return
	leave
	ret

gc_GCInit_v:
	; Create new stack frame
	push ebp
	mov ebp, esp
	; Save important registers
	push ebx
	push ecx

	; Allocate space for local variables and function call arguments
	sub esp, 8

	; lowerStackAddress = stackStart
	mov ebx, DWORD [ebp + 8]
	mov DWORD [gc_lowerStackAddress], ebx

	mov DWORD [gc_heapMemoryStart], __data_start
	mov DWORD [gc_heapMemoryEnd], _end
	; Push "heap start address: 0x%x\n" onto the stack
	mov DWORD [esp + 0], lc16
	
	; Push the address contained in variable 'heapMemoryStart' onto the stack
	mov ebx, DWORD [gc_heapMemoryStart + 0]
	mov DWORD [esp + 4], ebx
	
	; printf("heap start address: 0x%x\n", heapMemoryStart)
	call printf

	; Push "heap end address: 0x%x\n" onto the stack
	mov DWORD [esp + 0], lc17
	
	; Push the address contained in variable 'heapMemoryEnd' onto the stack
	mov ebx, DWORD [gc_heapMemoryEnd + 0]
	mov DWORD [esp + 4], ebx
	
	; printf("heap end address: 0x%x\n", heapMemoryEnd)
	call printf

.return:
	; Free up stack space
	add esp, 8
	; Reset registers
	pop ecx
	pop ebx
	; Reset stack frame and return
	leave
	ret


SECTION .bss
; void* lowerStackAddress
gc_lowerStackAddress: resb 4
; void* dataEndAddress
gc_dataEndAddress: resb 4
; void* heapMemoryStart
gc_heapMemoryStart: resb 4
; void* heapMemoryEnd
gc_heapMemoryEnd: resb 4
; heapMeta* heapMemory
gc_heapMemory: resb 4


SECTION .data


SECTION .rodata
lc16: db "heap start address: 0x%x", 0x0A, 0
lc17: db "heap end address: 0x%x", 0x0A, 0