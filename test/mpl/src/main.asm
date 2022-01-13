EXTERN malloc
EXTERN free
EXTERN __data_start
EXTERN _end
EXTERN scanf
EXTERN printf
EXTERN gc_GCInit_v
GLOBAL main_heapVariable
GLOBAL main

SECTION .text
main_gimmeInt_dd:
	; Create new stack frame
	push ebp
	mov ebp, esp
	; Save important registers
	push ebx
	push ecx

	; Allocate space for local variables and function call arguments
	sub esp, 4

	; return x + y
	; x + y
	mov ebx, DWORD [ebp + 12]
	mov eax, DWORD [ebp + 8]
	add eax, ebx
	mov DWORD [esp + 0], eax
	
	
	mov eax, [esp + 0]
	jmp .return

.return:
	; Free up stack space
	add esp, 4
	; Reset registers
	pop ecx
	pop ebx
	; Reset stack frame and return
	leave
	ret

main_Car_Ptr_setInfo_Sddd:
	; Create new stack frame
	push ebp
	mov ebp, esp
	; Save important registers
	push ebx
	push ecx

	; this.wheelSize = wheelSize
	mov ebx, DWORD [ebp + 12]
	mov ecx, [ebp + 8]
	; lea ecx, [ecx + 0 + 0]
	mov DWORD [ecx], ebx

	; this.speed = speed
	mov ebx, DWORD [ebp + 16]
	mov ecx, [ebp + 8]
	lea ecx, [ecx + 4 + 0]
	mov DWORD [ecx], ebx

	; this.fuelTank.capacity = fuelTankCapacity
	mov ebx, DWORD [ebp + 20]
	mov ecx, [ebp + 8]
	lea ecx, [ecx + 8]
	; lea ecx, [ecx + 0 + 0]
	mov DWORD [ecx], ebx

.return:
	; Free up stack space
	add esp, 0
	; Reset registers
	pop ecx
	pop ebx
	; Reset stack frame and return
	leave
	ret

main_Car_Ptr_printInfo_S:
	; Create new stack frame
	push ebp
	mov ebp, esp
	; Save important registers
	push ebx
	push ecx

	; Allocate space for local variables and function call arguments
	sub esp, 16

	; Push "wheel size: %d, speed: %d, fuel tank capacity: %d\n" onto the stack
	mov DWORD [esp + 0], lc0
	
	; Push the contents of variable 'this.wheelSize' onto the stack
	mov ebx, [ebp + 8]
	mov ebx, DWORD [ebx + 0 + 0]
	mov DWORD [esp + 4], ebx
	
	; Push the contents of variable 'this.speed' onto the stack
	mov ebx, [ebp + 8]
	mov ebx, DWORD [ebx + 4 + 0]
	mov DWORD [esp + 8], ebx
	
	; Push the contents of variable 'this.fuelTank.capacity' onto the stack
	mov ebx, [ebp + 8]
	lea ebx, [ebx + 8]
	mov ebx, DWORD [ebx + 0 + 0]
	mov DWORD [esp + 12], ebx
	
	; printf("wheel size: %d, speed: %d, fuel tank capacity: %d\n", this.wheelSize, this.speed, this.fuelTank.capacity)
	call printf

.return:
	; Free up stack space
	add esp, 16
	; Reset registers
	pop ecx
	pop ebx
	; Reset stack frame and return
	leave
	ret

main:
	; Create new stack frame
	push ebp
	mov ebp, esp

	; Allocate space for local variables and function call arguments
	sub esp, 140

	; Push 0 onto the stack
	mov DWORD [esp + 0], 0
	
	; GCInit(0)
	call gc_GCInit_v

	; a = 10
	mov DWORD [ebp - 111], 10

	; b = 5
	mov DWORD [ebp - 107], 5

	; a / b
	mov ebx, DWORD [ebp - 107]
	mov eax, DWORD [ebp - 111]
	cdq
	idiv ebx
	mov DWORD [esp + 8], eax
	
	; Push data from memory at address [esp + 8] onto the stack
	mov eax, DWORD [esp + 8]
	mov DWORD [esp + 0], eax
	
	; a / b
	mov ebx, DWORD [ebp - 107]
	mov eax, DWORD [ebp - 111]
	cdq
	idiv ebx
	mov DWORD [esp + 8], eax
	
	; [esp + 8] - 1
	mov ebx, 1
	sub DWORD [esp + 8], ebx
	
	; Push data from memory at address [esp + 8] onto the stack
	mov eax, DWORD [esp + 8]
	mov DWORD [esp + 4], eax
	
	; gimmeInt(a / b, a / b - 1)
	call main_gimmeInt_dd
	mov DWORD [esp + 8], eax
	
	; [esp + 8] * 123
	mov eax, 123
	imul DWORD [esp + 8]
	mov DWORD [esp + 8], eax
	
	; Push parentheses exp resultant onto the stack
	mov eax, DWORD [esp + 8]
	mov DWORD [esp + 8], eax
	
	; [esp + 8] / 13
	mov eax, DWORD [esp + 8]
	cdq
	mov ebx, 13
	idiv ebx
	mov DWORD [esp + 8], eax
	
	; b / 2
	mov eax, DWORD [ebp - 107]
	cdq
	mov ebx, 2
	idiv ebx
	mov DWORD [esp + 12], eax
	
	; [esp + 12] * 7
	mov eax, 7
	imul DWORD [esp + 12]
	mov DWORD [esp + 12], eax
	
	; [esp + 8] + [esp + 12]
	mov ebx, DWORD [esp + 12]
	add DWORD [esp + 8], ebx
	
	; Push parentheses exp resultant onto the stack
	mov eax, DWORD [esp + 8]
	mov DWORD [esp + 8], eax
	
	; a / b
	mov ebx, DWORD [ebp - 107]
	mov eax, DWORD [ebp - 111]
	cdq
	idiv ebx
	mov DWORD [esp + 12], eax
	
	; Push data from memory at address [esp + 12] onto the stack
	mov eax, DWORD [esp + 12]
	mov DWORD [esp + 0], eax
	
	; Push 1 onto the stack
	mov DWORD [esp + 4], 1
	
	; gimmeInt(a / b, 1)
	call main_gimmeInt_dd
	mov DWORD [esp + 12], eax
	
	; [esp + 12] * 123
	mov eax, 123
	imul DWORD [esp + 12]
	mov DWORD [esp + 12], eax
	
	; Push parentheses exp resultant onto the stack
	mov eax, DWORD [esp + 12]
	mov DWORD [esp + 12], eax
	
	; [esp + 12] / 13
	mov eax, DWORD [esp + 12]
	cdq
	mov ebx, 13
	idiv ebx
	mov DWORD [esp + 12], eax
	
	; b / 2
	mov eax, DWORD [ebp - 107]
	cdq
	mov ebx, 2
	idiv ebx
	mov DWORD [esp + 16], eax
	
	; [esp + 16] * 7
	mov eax, 7
	imul DWORD [esp + 16]
	mov DWORD [esp + 16], eax
	
	; [esp + 12] + [esp + 16]
	mov ebx, DWORD [esp + 16]
	add DWORD [esp + 12], ebx
	
	; Push parentheses exp resultant onto the stack
	mov eax, DWORD [esp + 12]
	mov DWORD [esp + 12], eax
	
	; [esp + 8] * [esp + 12]
	mov eax, DWORD [esp + 12]
	imul DWORD [esp + 8]
	mov DWORD [esp + 8], eax
	
	; c = ((gimmeInt(a / b, a / b - 1) * 123) / 13 + b / 2 * 7) * ((gimmeInt(a / b, 1) * 123) / 13 + b / 2 * 7)
	mov eax, [esp + 8]
	mov DWORD [ebp - 103], eax

	; Push "c: %d, b: %d\n" onto the stack
	mov DWORD [esp + 0], lc1
	
	; Push the contents of variable 'c' onto the stack
	mov ebx, DWORD [ebp - 103]
	mov DWORD [esp + 4], ebx
	
	; Push the contents of variable 'b' onto the stack
	mov ebx, DWORD [ebp - 107]
	mov DWORD [esp + 8], ebx
	
	; printf("c: %d, b: %d\n", c, b)
	call printf

	; boolTest = -123
	mov DWORD [ebp - 99], -123

	; test = boolTest
	mov ebx, DWORD [ebp - 99]
	cmp ebx, 0
	setg BYTE [ebp - 95]

	; if(test == 1) {
	; Condition testing (test == 1)
	movsx eax, BYTE [ebp - 95]
	cmp eax, 1
	jne .if0_else
	.if0:
		; Push "test\n" onto the stack
		mov DWORD [esp + 0], lc2
		
		; printf("test\n")
		call printf
	
		jmp .if0_end
	; }else{
	.if0_else:
		; Push "no test!\n" onto the stack
		mov DWORD [esp + 0], lc3
		
		; printf("no test!\n")
		call printf
	
	; }
	.if0_end:

	; bp = &test
	lea ebx, [ebp - 95]
	mov DWORD [ebp - 94], ebx

	; if(*bp == 1) {
	; Condition testing (*bp == 1)
	mov eax, DWORD [ebp - 94]
	movsx eax, BYTE [eax]
	cmp eax, 1
	jne .if1_end
	.if1:
		; Push "ayay\n" onto the stack
		mov DWORD [esp + 0], lc4
		
		; printf("ayay\n")
		call printf
	
	; }
	.if1_end:

	; car.wheelSize = 18
	mov DWORD [ebp - 90], 18

	; car.speed = 200
	mov DWORD [ebp - 86], 200

	; car.fuelTank.capacity = 68
	mov DWORD [ebp - 82], 68

	; Push "&car: %x\n" onto the stack
	mov DWORD [esp + 0], lc5
	
	; Push the address of variable '&car' onto the stack
	lea ebx, [ebp - 90]
	mov DWORD [esp + 4], ebx
	
	; printf("&car: %x\n", &car)
	call printf

	; Push "&car.greitis: %x\n" onto the stack
	mov DWORD [esp + 0], lc6
	
	; Push the address of variable '&car.speed' onto the stack
	lea ebx, [ebp - 90]
	lea ebx, [ebx + 4 + 0]
	mov DWORD [esp + 4], ebx
	
	; printf("&car.greitis: %x\n", &car.speed)
	call printf

	; Push the address of variable '&car' onto the stack
	lea ebx, [ebp - 90]
	mov DWORD [esp + 0], ebx
	
	; printInfo(&car)
	call main_Car_Ptr_printInfo_S

	; Push the address of variable '&car' onto the stack
	lea ebx, [ebp - 90]
	mov DWORD [esp + 0], ebx
	
	; Push 15 onto the stack
	mov DWORD [esp + 4], 15
	
	; Push 113 onto the stack
	mov DWORD [esp + 8], 113
	
	; Push 70 onto the stack
	mov DWORD [esp + 12], 70
	
	; setInfo(&car, 15, 113, 70)
	call main_Car_Ptr_setInfo_Sddd

	; Push the address of variable '&car' onto the stack
	lea ebx, [ebp - 90]
	mov DWORD [esp + 0], ebx
	
	; printInfo(&car)
	call main_Car_Ptr_printInfo_S

	; Push "\n" onto the stack
	mov DWORD [esp + 0], lc7
	
	; printf("\n")
	call printf

	; cPtr = &car
	lea ebx, [ebp - 90]
	lea ecx, [ebp - 78]
	mov DWORD [ecx], ebx

	; Push "*cPtr: %d, *(cPtr+8): %d\n" onto the stack
	mov DWORD [esp + 0], lc8
	
	; Push the contents of memory address, contained in variable '*cPtr' onto the stack
	mov ebx, DWORD [ebp - 78]
	mov ebx, DWORD [ebx]
	mov DWORD [esp + 4], ebx
	
	; cPtr + 8
	mov ebx, DWORD [ebp - 78]
	add ebx, 8
	mov DWORD [esp + 12], ebx
	
	; Dereference, referenced value
	mov eax, DWORD [esp + 12]
	mov eax, [eax]
	mov DWORD [esp + 12], eax
	; Push parentheses exp resultant onto the stack
	mov eax, DWORD [esp + 12]
	mov DWORD [esp + 12], eax
	
	; Push data from memory at address [esp + 12] onto the stack
	mov eax, DWORD [esp + 12]
	mov DWORD [esp + 8], eax
	
	; printf("*cPtr: %d, *(cPtr+8): %d\n", *cPtr, *(cPtr + 8))
	call printf

	; Push the address of variable '&cs[0]' onto the stack
	lea ebx, [ebp + 0 - 74]
	mov DWORD [esp + 0], ebx
	
	; Push 11 onto the stack
	mov DWORD [esp + 4], 11
	
	; Push 111 onto the stack
	mov DWORD [esp + 8], 111
	
	; Push 1111 onto the stack
	mov DWORD [esp + 12], 1111
	
	; setInfo(&cs[0], 11, 111, 1111)
	call main_Car_Ptr_setInfo_Sddd

	; Push the address of variable '&cs[1]' onto the stack
	lea ebx, [ebp + 12 - 74]
	mov DWORD [esp + 0], ebx
	
	; Push 23 onto the stack
	mov DWORD [esp + 4], 23
	
	; Push 232 onto the stack
	mov DWORD [esp + 8], 232
	
	; Push 3232 onto the stack
	mov DWORD [esp + 12], 3232
	
	; setInfo(&cs[1], 23, 232, 3232)
	call main_Car_Ptr_setInfo_Sddd

	; i = 0
	mov DWORD [ebp - 50], 0

	; while(i < 2) {
	.while0_condition:
	; Condition testing (i < 2)
	mov eax, 2
	mov ebx, DWORD [ebp - 50]
	cmp eax, ebx
	jle .while0_done
	.while0:
		; Push "&cs[%d]: %x\n" onto the stack
		mov DWORD [esp + 0], lc9
		
		; Push the contents of variable 'i' onto the stack
		mov ebx, DWORD [ebp - 50]
		mov DWORD [esp + 4], ebx
		
		; Push the address of variable '&cs[i]' onto the stack
		mov ebx, DWORD [ebp - 50]
		imul ebx, 12
		lea ebx, [ebp + ebx - 74]
		mov DWORD [esp + 8], ebx
		
		; printf("&cs[%d]: %x\n", i, &cs[i])
		call printf
	
		; i++
		lea eax, [ebp - 50]
		inc DWORD [eax]
	jmp .while0_condition
	; }
	.while0_done:

	; for (nullz = 0; z < 2; z++) {
	.for0_init:
	; z = 0
	mov DWORD [ebp - 16], 0
	.for0_condition:
	; Condition testing (z < 2)
	mov eax, 2
	mov ebx, DWORD [ebp - 16]
	cmp eax, ebx
	jle .for0_end
		; Push the address of variable '&cs[z]' onto the stack
		mov ebx, DWORD [ebp - 16]
		imul ebx, 12
		lea ebx, [ebp + ebx - 74]
		mov DWORD [esp + 0], ebx
		
		; printInfo(&cs[z])
		call main_Car_Ptr_printInfo_S
	.for0_inc:
		; z++
		lea eax, [ebp - 16]
		inc DWORD [eax]
		; Jmp back to condition testing
		jmp .for0_condition
	.for0_end:

	; for Car carr in Car[] cs
	mov ebx, 0
	.forE0:
	cmp ebx, 2
	je .forE0_end
	; carr = cs[ebx]
	mov ecx, ebx
	imul ecx, 12
	mov ecx, DWORD [ebp + ecx - 74]
	mov DWORD [ebp - 12], ecx
	mov ecx, ebx
	imul ecx, 12
	mov ecx, DWORD [ebp + ecx - 70]
	mov DWORD [ebp - 8], ecx
	mov ecx, ebx
	imul ecx, 12
	mov ecx, DWORD [ebp + ecx - 66]
	mov DWORD [ebp - 4], ecx
	
	; {
		; Push the address of variable '&carr' onto the stack
		lea ecx, [ebp - 12]
		mov DWORD [esp + 0], ecx
		
		; printInfo(&carr)
		call main_Car_Ptr_printInfo_S
	
	; }
	; Jmp back to beginning
	inc ebx
	jmp .forE0
	.forE0_end:

	; Push "hello: " onto the stack
	mov DWORD [esp + 0], lc10
	
	; Push the address of variable '&testStr' onto the stack
	lea ecx, [ebp - 46]
	mov DWORD [esp + 4], ecx
	
	; scanf("hello: ", &testStr)
	call main_scanf

	; Push "output: %s\n" onto the stack
	mov DWORD [esp + 0], lc11
	
	; Push the address of variable '&testStr' onto the stack
	lea ecx, [ebp - 46]
	mov DWORD [esp + 4], ecx
	
	; printf("output: %s\n", &testStr)
	call printf

.return:
	; Free up stack space
	add esp, 140
	xor eax, eax
	; Reset stack frame and return
	leave
	ret


SECTION .bss


SECTION .data
; int heapVariable
main_heapVariable: dd 5


SECTION .rodata
lc8: db "*cPtr: %d, *(cPtr+8): %d", 0x0A, 0
lc7: db "", 0x0A, 0
lc9: db "&cs[%d]: %x", 0x0A, 0
lc10: db "hello: ", 0
lc11: db "output: %s", 0x0A, 0
lc0: db "wheel size: %d, speed: %d, fuel tank capacity: %d", 0x0A, 0
lc2: db "test", 0x0A, 0
lc1: db "c: %d, b: %d", 0x0A, 0
lc4: db "ayay", 0x0A, 0
lc3: db "no test!", 0x0A, 0
lc6: db "&car.greitis: %x", 0x0A, 0
lc5: db "&car: %x", 0x0A, 0